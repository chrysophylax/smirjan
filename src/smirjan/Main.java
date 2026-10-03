package smirjan;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** {@code smirjan [--tsv] [--assign=LIST] <definitions.def> <count>}: prints distinct words, one per line. */
public final class Main {
    private Main() {}

    public static void main(String[] args) {
        boolean tsvOutput = false;
        Meanings assign = null;
        List<String> positional = new ArrayList<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            switch (a) {
                case "-h", "--help" -> {
                    usage(System.out);
                    return;
                }
                case "--tsv" -> tsvOutput = true;
                case "--assign" -> {
                    if (i + 1 >= args.length) {
                        fail("--assign needs a list: lpj, dlg or wlt");
                    }
                    assign = meaningList(args[++i]);
                }
                default -> {
                    if (a.startsWith("--assign=")) {
                        assign = meaningList(a.substring("--assign=".length()));
                        continue;
                    }
                    if (a.startsWith("-") && a.length() > 1 && !a.matches("-\\d+")) {
                        System.err.println("smirjan: unknown option '" + a + "'");
                        usage(System.err);
                        System.exit(2);
                    }
                    positional.add(a);
                }
            }
        }
        if (positional.size() != 2) {
            usage(System.err);
            System.exit(2);
        }
        int count;
        try {
            count = Integer.parseInt(positional.get(1));
            if (count < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            System.err.println("smirjan: word count must be a non-negative whole number, got '"
                    + positional.get(1) + "'");
            System.exit(2);
            return;
        }

        Path file = Path.of(positional.get(0));
        Definition def;
        try {
            def = DefinitionParser.parse(file);
        } catch (NoSuchFileException e) {
            System.err.println("smirjan: no such file: " + file);
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("smirjan: cannot read " + file + ": " + e.getMessage());
            System.exit(1);
            return;
        } catch (DefinitionException e) {
            System.err.println("smirjan: " + file + ": " + e.getMessage());
            System.exit(1);
            return;
        }
        if (!def.settings.seedGiven) {
            System.err.println("smirjan: no seed given; using seed: " + def.settings.seed);
        }

        PrintStream out = new PrintStream(
                new BufferedOutputStream(new FileOutputStream(FileDescriptor.out)), false, StandardCharsets.UTF_8);
        Generator gen = new Generator(def);
        Tsv tsv = tsvOutput ? new Tsv(def, assign != null) : null;
        if (tsv != null) {
            out.println(tsv.header());
        }
        int made;
        if (assign == null) {
            if (tsv != null) {
                made = gen.generateWords(count, w -> out.println(tsv.row(w, null, "", parts(w))));
            } else {
                made = gen.generate(count, out::println);
            }
        } else {
            List<Lexicon.Entry> entries = Lexicon.build(def, gen, assign, count);
            for (Lexicon.Entry e : entries) {
                List<String> glosses = e.parts().stream().map(Lexicon.Entry::gloss).toList();
                if (tsv != null) {
                    out.println(tsv.row(e.word(), e.number(), e.gloss(), glosses));
                } else {
                    out.println(e.word().text() + "\t" + e.gloss()
                            + (glosses.isEmpty() ? "" : "\t= " + String.join(" + ", glosses)));
                }
            }
            made = (int) entries.stream().filter(Lexicon.Entry::requested).count();
            int size = assign.load().size();
            if (made > size) {
                System.err.println("smirjan: the " + assign.title + " has " + size + " meanings; "
                        + (made - size) + " words were left unassigned");
            }
        }
        out.flush();
        if (made < count) {
            System.err.println("smirjan: only " + made + " distinct words could be generated from " + file);
            System.exit(3);
        }
    }

    private static List<String> parts(Generator.Word w) {
        return w.compound() == null ? List.of() : w.compound().parts().stream().map(Generator.Word::text).toList();
    }

    private static Meanings meaningList(String name) {
        try {
            return Meanings.parse(name);
        } catch (IllegalArgumentException e) {
            fail(e.getMessage());
            return null;
        }
    }

    private static void fail(String message) {
        System.err.println("smirjan: " + message);
        usage(System.err);
        System.exit(2);
    }

    private static void usage(PrintStream p) {
        p.println("usage: smirjan [--tsv] [--assign=lpj|dlg|wlt] <definitions.def> <count>");
        p.println("Generates <count> distinct words from the phonology in <definitions.def>.");
        p.println();
        p.println("  --tsv   tab-separated output with a header row and one row per word:");
        p.println("          " + Tsv.HEADER.replace('\t', ' '));
        p.println("  --assign=LIST");
        p.println("          give each word a meaning from a standard list, chosen by the seed,");
        p.println("          and print the words in list order:");
        p.println("            lpj  Leipzig-Jakarta list (100)");
        p.println("            dlg  Dolgopolsky list (15)");
        p.println("            wlt  Loanword Typology / WOLD meaning list (1460)");
    }
}
