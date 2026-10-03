import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Regenerates the bundled meaning data in src/smirjan/meanings/ from pinned
 * releases of Concepticon and NoRaRe (CLDF), both CC BY 4.0. Run from the
 * repository root:
 *
 * <pre>java tools/BuildMeaningData.java</pre>
 *
 * Outputs:
 * <ul>
 * <li>lpj.tsv, dlg.tsv, wlt.tsv: the meaning lists (number, Concepticon ID, gloss)</li>
 * <li>concepts.tsv: every concept on any list, with gloss, semantic field,
 *     ontological category, simplicity and CLICS colexification community</li>
 * <li>relations.tsv: part/whole and broader/narrower relations (Concepticon),
 *     and meanings derived from other meanings (Urban 2011)</li>
 * </ul>
 */
public class BuildMeaningData {
    static final String CONCEPTICON = "https://raw.githubusercontent.com/concepticon/concepticon-data/"
            + "918bc44e123952a6ab5733be36c2d463799c23b4/concepticondata/"; // v3.4.0
    static final String NORARE = "https://github.com/concepticon/norare-cldf/raw/"
            + "895b055b9e5dd339bbd2b67509802d85aea72126/cldf/"; // v1.1
    static final String COMMUNITY = "Rzymski-2020-1624-COMMUNITY";

    static final Path OUT = Path.of("src/smirjan/meanings");

    record Concept(int id, String gloss, String field, String category, String simplicity, String community) {}

    public static void main(String[] args) throws Exception {
        Files.createDirectories(OUT);
        Files.deleteIfExists(OUT.resolve("associations.tsv")); // from an earlier, non-CC-BY source

        // The meaning lists, and the universe of concepts they cover.
        Map<Integer, String> glossOf = new LinkedHashMap<>();
        // How often languages express a meaning with a simple (unanalysable) word:
        // WOLD's SIMPLICITY_SCORE, or the Leipzig-Jakarta ANALYZABILITY_SCORE.
        Map<Integer, String> simplicity = new HashMap<>();
        String[][] lists = {
                {"lpj", "Tadmor-2009-100", "Leipzig-Jakarta list (Tadmor 2009)"},
                {"dlg", "Dolgopolsky-1964-15", "Dolgopolsky list (Dolgopolsky 1964)"},
                {"wlt", "Haspelmath-2009-1460", "Loanword Typology / WOLD meaning list (Haspelmath & Tadmor 2009)"},
        };
        for (String[] l : lists) {
            List<Map<String, String>> rows = tsv(fetch(CONCEPTICON + "conceptlists/" + l[1] + ".tsv"));
            rows.sort((a, b) -> Integer.compare(Integer.parseInt(a.get("NUMBER")), Integer.parseInt(b.get("NUMBER"))));
            StringBuilder sb = new StringBuilder();
            sb.append("# ").append(l[2]).append('\n');
            sb.append("# Source: Concepticon conceptlist ").append(l[1])
                    .append(" (https://concepticon.clld.org, v3.4.0), CC BY 4.0\n");
            sb.append("# number\tconcepticon_id\tgloss\n");
            for (Map<String, String> r : rows) {
                String gloss = r.get("ENGLISH").strip().replaceAll("\\s+", " ");
                String cid = r.get("CONCEPTICON_ID");
                sb.append(Integer.parseInt(r.get("NUMBER"))).append('\t').append(cid).append('\t').append(gloss).append('\n');
                if (!cid.isEmpty()) {
                    int id = Integer.parseInt(cid);
                    glossOf.putIfAbsent(id, stripArticle(gloss)); // lpj, then dlg, then wlt
                    String score = r.getOrDefault("SIMPLICITY_SCORE", r.get("ANALYZABILITY_SCORE"));
                    if (score != null && !score.isBlank() && !score.equals("None")) {
                        // WOLD comes last and is the larger sample, so it wins.
                        simplicity.put(id, String.format(Locale.ROOT, "%.3f", Double.parseDouble(score)));
                    }
                }
            }
            write(l[0] + ".tsv", sb.toString());
            System.out.println(l[0] + ": " + rows.size() + " meanings");
        }

        // CLICS colexification communities (Rzymski et al. 2020), via NoRaRe.
        Map<String, Integer> unitConcept = new HashMap<>();
        forEachLine(fetchBytes(NORARE + "glosses.csv"), null, line -> {
            if (line.startsWith("Rzymski-2020-1624-")) {
                String[] c = line.split(",", 4); // ID,Language_ID,Parameter_ID,...
                if (!c[2].isEmpty()) {
                    unitConcept.put(c[0], Integer.parseInt(c[2]));
                }
            }
        });
        Map<Integer, String> community = new HashMap<>();
        forEachLine(fetchBytes(NORARE + "norare.csv.zip"), "norare.csv", line -> {
            String[] c = line.split(",", 4); // ID,Unit_ID,Variable_ID,Value
            if (c.length == 4 && c[2].equals(COMMUNITY) && unitConcept.containsKey(c[1])) {
                community.put(unitConcept.get(c[1]), c[3].replace("\"", ""));
            }
        });

        Map<Integer, Concept> concepts = new TreeMap<>();
        for (Map<String, String> r : tsv(fetch(CONCEPTICON + "concepticon.tsv"))) {
            int id = Integer.parseInt(r.get("ID"));
            if (glossOf.containsKey(id)) {
                concepts.put(id, new Concept(id, glossOf.get(id), r.get("SEMANTICFIELD"), r.get("ONTOLOGICAL_CATEGORY"),
                        simplicity.getOrDefault(id, ""), community.getOrDefault(id, "")));
            }
        }
        StringBuilder cs = new StringBuilder("""
                # Concepts on the bundled meaning lists. All sources CC BY 4.0:
                #   gloss, semantic field, ontological category: Concepticon (https://concepticon.clld.org, v3.4.0)
                #   simplicity: share of languages expressing the meaning with a simple word
                #     (WOLD SIMPLICITY_SCORE, else Leipzig-Jakarta ANALYZABILITY_SCORE; Haspelmath & Tadmor 2009)
                #   community: CLICS colexification community (Rzymski et al. 2020), via NoRaRe
                #     (Tjuka, Forkel & List 2022; https://norare.clld.org, norare-cldf v1.1)
                # concepticon_id\tgloss\tsemantic_field\tontological_category\tsimplicity\tcommunity
                """);
        concepts.values().forEach(c -> cs.append(c.id).append('\t').append(c.gloss).append('\t')
                .append(c.field).append('\t').append(c.category).append('\t').append(c.simplicity)
                .append('\t').append(c.community).append('\n'));
        write("concepts.tsv", cs.toString());

        // Concepticon relations, restricted to concepts on the lists.
        Map<Integer, Map<Integer, String>> rel = new TreeMap<>();
        for (Map<String, String> r : tsv(fetch(CONCEPTICON + "conceptrelations.tsv"))) {
            int s = Integer.parseInt(r.get("SOURCE"));
            int t = Integer.parseInt(r.get("TARGET"));
            if (!concepts.containsKey(s) || !concepts.containsKey(t) || s == t) {
                continue;
            }
            switch (r.get("RELATION")) {
                case "partof" -> { // SOURCE has TARGET as a part
                    link(rel, s, t, "part", true);
                    link(rel, t, s, "whole", true);
                }
                case "narrower", "instanceof" -> { // TARGET is a kind of SOURCE
                    link(rel, s, t, "narrower", true);
                    link(rel, t, s, "broader", true);
                }
                case "similar" -> {
                    link(rel, s, t, "similar", false);
                    link(rel, t, s, "similar", false);
                }
                default -> { }
            }
        }
        // Urban (2011): meanings that languages overtly form from other meanings.
        List<Map<String, String>> urban = tsv(fetch(CONCEPTICON + "conceptlists/Urban-2011-160.tsv"));
        Map<String, Integer> urbanIds = new HashMap<>();
        for (Map<String, String> r : urban) {
            urbanIds.put(r.get("ID"), Integer.parseInt(r.get("CONCEPTICON_ID")));
        }
        Pattern target = Pattern.compile("\"ID\": \"([^\"]+)\", \"NAME\": \"[^\"]*\", \"OvertMarking\": (\\d+)");
        int derived = 0;
        for (Map<String, String> r : urban) {
            int s = Integer.parseInt(r.get("CONCEPTICON_ID"));
            Matcher m = target.matcher(r.getOrDefault("TARGET_CONCEPTS", ""));
            while (m.find()) {
                Integer t = urbanIds.get(m.group(1));
                if (t != null && t != s && Integer.parseInt(m.group(2)) > 0
                        && concepts.containsKey(s) && concepts.containsKey(t)) {
                    link(rel, t, s, "derived", false);
                    derived++;
                }
            }
        }
        StringBuilder rs = new StringBuilder("""
                # Relations between concepts on the bundled lists. All sources CC BY 4.0:
                #   part, whole, broader, narrower, similar: Concepticon conceptrelations
                #     (https://concepticon.clld.org, v3.4.0)
                #   derived: the concept is formed from this one in several languages
                #     (Urban 2011, via Concepticon conceptlist Urban-2011-160)
                # concepticon_id\trelated (id:kind)
                """);
        rel.forEach((id, m) -> {
            StringBuilder line = new StringBuilder();
            m.forEach((t, k) -> line.append(line.isEmpty() ? "" : ",").append(t).append(':').append(k));
            rs.append(id).append('\t').append(line).append('\n');
        });
        write("relations.tsv", rs.toString());
        System.out.println(concepts.size() + " concepts, " + concepts.values().stream().filter(c -> !c.community.isEmpty()).count()
                + " in CLICS communities, " + rel.size() + " with relations, " + derived + " derivations");
    }

    /** Records that {@code from} relates to {@code to} as {@code kind}, replacing any earlier kind if {@code overwrite}. */
    static void link(Map<Integer, Map<Integer, String>> rel, int from, int to, String kind, boolean overwrite) {
        Map<Integer, String> links = rel.computeIfAbsent(from, k -> new TreeMap<>());
        if (overwrite) {
            links.put(to, kind);
        } else {
            links.putIfAbsent(to, kind);
        }
    }

    /** "the bone" -> "bone"; list glosses otherwise stay as published. */
    static String stripArticle(String gloss) {
        return gloss.startsWith("the ") ? gloss.substring(4) : gloss;
    }

    static String fetch(String url) throws IOException, InterruptedException {
        return new String(fetchBytes(url), StandardCharsets.UTF_8);
    }

    static byte[] fetchBytes(String url) throws IOException, InterruptedException {
        HttpResponse<byte[]> r = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
                .send(HttpRequest.newBuilder(URI.create(url)).build(), HttpResponse.BodyHandlers.ofByteArray());
        if (r.statusCode() != 200) {
            throw new IOException(url + ": HTTP " + r.statusCode());
        }
        return r.body();
    }

    /** Streams the lines of a text file, or of one entry of a zip file. */
    static void forEachLine(byte[] data, String zipEntry, java.util.function.Consumer<String> each) throws IOException {
        var in = new ByteArrayInputStream(data);
        if (zipEntry == null) {
            new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)).lines().forEach(each);
            return;
        }
        try (ZipInputStream zip = new ZipInputStream(in)) {
            for (ZipEntry e; (e = zip.getNextEntry()) != null; ) {
                if (e.getName().endsWith(zipEntry)) {
                    new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8)).lines().forEach(each);
                    return;
                }
            }
        }
        throw new IOException("no " + zipEntry + " in archive");
    }

    /** A TSV table with a header row; fields may be double-quoted. */
    static List<Map<String, String>> tsv(String text) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder f = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    f.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    f.append(c);
                }
            } else if (c == '"' && f.isEmpty()) {
                quoted = true;
            } else if (c == '\t') {
                row.add(f.toString());
                f.setLength(0);
            } else if (c == '\n') {
                row.add(f.toString().replace("\r", ""));
                f.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else {
                f.append(c);
            }
        }
        if (!f.isEmpty() || !row.isEmpty()) {
            row.add(f.toString());
            rows.add(row);
        }
        List<String> header = rows.getFirst();
        List<Map<String, String>> out = new ArrayList<>();
        for (List<String> r : rows.subList(1, rows.size())) {
            if (r.size() == 1 && r.getFirst().isBlank()) {
                continue;
            }
            Map<String, String> m = new HashMap<>();
            for (int i = 0; i < header.size() && i < r.size(); i++) {
                m.put(header.get(i), r.get(i));
            }
            out.add(m);
        }
        return out;
    }

    static void write(String name, String content) throws IOException {
        Files.writeString(OUT.resolve(name), content, StandardCharsets.UTF_8);
    }
}
