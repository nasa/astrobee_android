
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

package gov.nasa.arc.astrobee;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public interface PendingResult {
    enum Status {
        QUEUED(0),
        EXECUTING(1),
        REQUEUED(2),
        COMPLETED(3);

        private final byte m_value;

        Status(final int value) {
            m_value = (byte) value;
        }

        public byte getValue() {
            return m_value;
        }

        public static Status fromValue(byte value) {
            for (Status s : Status.values()) {
                if (s.m_value == value)
                    return s;
            }
            throw new IllegalArgumentException("No such Status with that value");
        }

        @Override
        public String toString() {
            return this.name() + "(" + m_value + ")";
        }
    }

    boolean isFinished();

    Status getStatus();

    Result getResult() throws AstrobeeException, InterruptedException;

    Result getResult(long timeout, TimeUnit unit) throws AstrobeeException, InterruptedException, TimeoutException;
}
