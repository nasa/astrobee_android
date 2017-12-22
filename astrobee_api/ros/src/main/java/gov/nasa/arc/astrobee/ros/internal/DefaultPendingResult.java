
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

import ff_msgs.AckStamped;
import ff_msgs.CommandStamped;
import gov.nasa.arc.astrobee.AstrobeeException;
import gov.nasa.arc.astrobee.PendingResult;
import gov.nasa.arc.astrobee.Result;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class DefaultPendingResult implements PendingResult {
    private Status m_status = Status.EXECUTING;

    // Synchronization object protecting the result and exception
    private final Object m_sync = new Object();
    private DefaultResult m_result = null;
    private AstrobeeException m_exception = null;

    private final CountDownLatch m_latch = new CountDownLatch(1);

    private final CommandStamped m_cmd;

    DefaultPendingResult(final CommandStamped cmd) {
        m_cmd = cmd;
    }

    CommandStamped getCommand() {
        return m_cmd;
    }

    @Override
    public synchronized boolean isFinished() {
        return m_status == Status.COMPLETED;
    }

    @Override
    public synchronized Status getStatus() {
        return m_status;
    }

    void update(final AckStamped ack) {
        setStatus(Status.fromValue(ack.getStatus().getStatus()));
        if (m_status == Status.COMPLETED)
            setResult(new DefaultResult(ack));
    }

    synchronized void setStatus(Status status) {
        m_status = status;
    }

    void setResult(final DefaultResult r) {
        synchronized (m_sync) {
            // If the count has hit zero, then someone has already set either
            // a result or an exception, so bail.
            if (m_latch.getCount() == 0)
                return;

            // We are the first, set our result/exception.
            m_result = r;
            m_latch.countDown();
        }
    }

    void setThrowable(final Throwable t) {
        synchronized (m_sync) {
            if (m_latch.getCount() == 0)
                return;
            if (t instanceof AstrobeeException)
                m_exception = (AstrobeeException) t;
            else
                m_exception = new AstrobeeException(t);
            m_latch.countDown();
        }
    }

    @Override
    public Result getResult() throws AstrobeeException, InterruptedException {
        m_latch.await();
        if (m_exception != null)
            throw m_exception;
        return m_result;
    }

    @Override
    public Result getResult(long timeout, TimeUnit unit) throws AstrobeeException, InterruptedException, TimeoutException {
        m_latch.await(timeout, unit);
        if (m_exception != null)
            throw m_exception;
        return m_result;
    }
}
