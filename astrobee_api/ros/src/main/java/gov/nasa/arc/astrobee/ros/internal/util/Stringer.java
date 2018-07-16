
/* Copyright (c) 2017, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 *
 * All rights reserved.
 *
 * The Astrobee platform is licensed under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package gov.nasa.arc.astrobee.ros.internal.util;

import ff_msgs.*;
import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.Result;

import java.util.*;

public final class Stringer {
    private Stringer() { }

    public static String toString(final GuestScienceConfig config) {
        if (config == null) {
            return "GuestScienceConfig{null}";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("GuestScienceConfig{");
        for (GuestScienceApk apk : config.getApks()) {
            sb.append("Apk{");
            sb.append("apkName=").append(apk.getApkName()).append("; ");
            sb.append("shortName=").append(apk.getShortName()).append("; ");
            sb.append("primary=").append(apk.getPrimary()).append("; ");
            for (GuestScienceCommand cmd : apk.getCommands()) {
                sb.append("GuestScienceCommand{");
                sb.append("name=").append(cmd.getName()).append("; ");
                sb.append("command=").append(cmd.getCommand());
                sb.append("} ");
            }
            sb.append("}; ");
        }
        sb.append("}");
        return sb.toString();
    }



    public static String toString(final AckStamped ack) {
        if (ack == null) {
            return "Ack{null}";
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("Ack{");
        sb.append("id=").append(ack.getCmdId()).append("; ");

        PendingResult.Status status = null;
        sb.append("status=");
        try {
            status = PendingResult.Status.fromValue(ack.getStatus().getStatus());
            sb.append(status.toString());
        } catch (IllegalArgumentException e) {
            sb.append("UNKNOWN(")
              .append(ack.getStatus().getStatus()).append(")");
        }

        if (status == PendingResult.Status.COMPLETED) {
            sb.append("; ")
              .append("completedStatus=");
            Result.Status cs;
            try {
                cs = Result.Status.fromValue(ack.getCompletedStatus().getStatus());
                sb.append(cs.toString());
            } catch (IllegalArgumentException e) {
                sb.append("UNKNOWN(")
                  .append(ack.getCompletedStatus().getStatus()).append(")");
            }
        }

        final String msg = ack.getMessage();
        if (msg != null && msg.length() > 0) {
            sb.append("; ");
            sb.append("message=\"").append(msg).append('"');
        }

        return sb.append('}').toString();
    }

    public static String toString(final CommandStamped cmd) {
        final StringBuilder sb = new StringBuilder();

        sb.append("Command{");

        sb.append("id=").append(cmd.getCmdId()).append("; ");
        sb.append("name=").append(cmd.getCmdName()).append("; ");
        sb.append("src=").append(cmd.getCmdSrc()).append("; ");
        sb.append("origin=").append(cmd.getCmdOrigin()).append("; ");

        String subsystem = cmd.getSubsysName();
        if (subsystem != null && subsystem.length() > 0) {
            sb.append("subsystem=").append(cmd.getSubsysName()).append("; ");
        }

        List<CommandArg> args = cmd.getArgs();
        sb.append("args=[");
        Iterator<CommandArg> it = args.iterator();
        while (it.hasNext()) {
            sb.append(toString(it.next()));
            if (it.hasNext())
                sb.append(", ");
        }
        sb.append("]");

        return sb.append('}').toString();
    }

    private static final String s_dataTypes[] = {
            "BOOL(0)", "DOUBLE(1)", "FLOAT(2)", "INT(3)", "LONG(4)",
            "STRING(5)", "VEC3d(6)", "MAT33f(7)"
    };

    private static String toString(final CommandArg arg) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Arg{");

        final byte type = arg.getDataType();
        if (type < s_dataTypes.length)
            sb.append(s_dataTypes[type]);
        else
            sb.append("UNKNOWN(").append(type).append(")");
        sb.append(": ");

        switch (type) {
            case CommandArg.DATA_TYPE_BOOL:
                sb.append(arg.getB());
                break;
            case CommandArg.DATA_TYPE_DOUBLE:
                sb.append(arg.getD());
                break;
            case CommandArg.DATA_TYPE_FLOAT:
                sb.append(arg.getF());
                break;
            case CommandArg.DATA_TYPE_INT:
                sb.append(arg.getI());
                break;
            case CommandArg.DATA_TYPE_LONGLONG:
                sb.append(arg.getLl());
                break;
            case CommandArg.DATA_TYPE_MAT33f:
                sb.append(Arrays.toString(arg.getMat33f()));
                break;
            case CommandArg.DATA_TYPE_STRING:
                sb.append(arg.getS());
                break;
            case CommandArg.DATA_TYPE_VEC3d:
                sb.append(Arrays.toString(arg.getVec3d()));
                break;
            default:
                sb.append("UNKNOWN");
                break;
        }

        return sb.append('}').toString();
    }
}
