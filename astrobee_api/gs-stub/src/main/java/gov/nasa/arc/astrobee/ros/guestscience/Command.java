package gov.nasa.arc.astrobee.ros.guestscience;

public class Command {
    private String name;
    private String syntax;

    public Command() { }

    public Command(String name, String syntax) {
        this.name = name;
        this.syntax = syntax;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSyntax() {
        return syntax;
    }

    public void setSyntax(String syntax) {
        this.syntax = syntax;
    }
}
