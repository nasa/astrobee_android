
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

package gov.nasa.arc.astrobee.ros.internal;

import ff_msgs.CommandArg;
import ff_msgs.CommandStamped;
import gov.nasa.arc.astrobee.internal.CommandBuilder;
import gov.nasa.arc.astrobee.internal.Publishable;
import gov.nasa.arc.astrobee.types.Mat33f;
import gov.nasa.arc.astrobee.types.Vec3d;
import org.ros.message.MessageFactory;
import std_msgs.Header;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class DefaultCommandBuilder implements CommandBuilder {
    private final MessageFactory m_messageFactory;
    private final CommandStamped m_cmd;
    private final List<CommandArg> m_args = new ArrayList<>();

    DefaultCommandBuilder(final MessageFactory messageFactory) {
        m_messageFactory = messageFactory;
        m_cmd = m_messageFactory.newFromType(CommandStamped._TYPE);
        m_cmd.setHeader((Header) m_messageFactory.newFromType(Header._TYPE));
        m_cmd.setCmdOrigin("guest_science");
    }

    @Override
    public CommandBuilder setName(final String name) {
        m_cmd.setCmdName(name);
        return this;
    }

    @Override
    public CommandBuilder setSubsystem(final String subsystem) {
        m_cmd.setSubsysName(subsystem);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, int value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_INT);
        arg.setI(value);
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, long value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_LONGLONG);
        arg.setLl(value);
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, float value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_FLOAT);
        arg.setF(value);
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, double value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_DOUBLE);
        arg.setD(value);
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, boolean value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_BOOL);
        arg.setB(value);
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, final Vec3d value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_VEC3d);
        arg.setVec3d(value.toArray());
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, final Mat33f value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_MAT33f);
        arg.setMat33f(value.toArray());
        m_args.add(arg);
        return this;
    }

    @Override
    public CommandBuilder addArgument(final String name, final String value) {
        final CommandArg arg = m_messageFactory.newFromType(CommandArg._TYPE);
        arg.setDataType(CommandArg.DATA_TYPE_STRING);
        arg.setS(value);
        m_args.add(arg);
        return this;
    }

    @Override
    public <E extends Enum<E>>
    CommandBuilder addArgument(final String name, final E value) {
        return addArgument(name, value.toString());
    }

    @Override
    public Publishable build() {
        m_cmd.setCmdId(UUID.randomUUID().toString());
        m_cmd.setArgs(m_args);
        return new CommandHolder(m_cmd);
    }
}
