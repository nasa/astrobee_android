package gov.nasa.arc.astrobee.ros.guestscience;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.arc.astrobee.ros.guestscience.Command;

public class ApkInfo {
    private String fullName;
    private String shortName;
    private boolean primary;
    private List<Command> commands = new ArrayList();

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getShortName() {
        return shortName;
    }

    public void setShortName(String shortName) {
        this.shortName = shortName;
    }

    public boolean isPrimary() {
        return primary;
    }

    public void setPrimary(boolean primary) {
        this.primary = primary;
    }

    public List getCommands() {
        return commands;
    }

    public void setCommands(List<Command> commands) {
        this.commands = commands;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FullName = " + fullName + ";\n");
        sb.append("ShortName = " + shortName + ";\n");
        sb.append("Primary = " + primary + ";\n");
        sb.append("Commands = {");
        for(Command c : commands) {
            sb.append(c.getName() + ", " + c.getSyntax() + "; ");
        }
        sb.append("}");


        return sb.toString();
    }
}
