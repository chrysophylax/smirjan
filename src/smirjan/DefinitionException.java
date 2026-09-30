package smirjan;

/** A problem in a .def file, reported to the user with its line number where known. */
final class DefinitionException extends Exception {
    private static final long serialVersionUID = 1L;

    DefinitionException(String message) {
        super(message);
    }

    static DefinitionException at(int line, String message) {
        return new DefinitionException("line " + line + ": " + message);
    }
}
