/*
 * Copyright 2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.mongodb.connection;

import com.sun.jna.platform.win32.Sspi.SecBufferDesc;
import com.mongodb.internal.waffle.windows.auth.IWindowsCredentialsHandle;
import com.mongodb.internal.waffle.windows.auth.impl.WindowsCredentialsHandleImpl;
import com.mongodb.internal.waffle.windows.auth.impl.WindowsSecurityContextImpl;

import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;

import static com.mongodb.assertions.Assertions.isTrueArgument;
import static com.mongodb.assertions.Assertions.notNull;
import static com.sun.jna.platform.win32.Sspi.SECBUFFER_TOKEN;

final class WindowsGSSAPI {

    public static SaslClient createSaslClient(final String userName, final String serviceName, final String hostName,
                                              final String serviceRealm) {
        return new SSPISaslClient(userName, serviceName, hostName, serviceRealm);
    }

    private WindowsGSSAPI() {
    }

    private static class SSPISaslClient implements SaslClient {
        private static final String SECURITY_PACKAGE = "Kerberos";

        private final String authorizationId;
        private final WindowsSecurityContextImpl clientContext;
        private final String servicePrincipalName;

        public SSPISaslClient(final String userName, final String serviceName, final String hostName,
                              final String serviceRealm) {
            notNull("userName", userName);
            notNull("serviceName", serviceName);
            notNull("hostName", hostName);
            authorizationId = userName;
            servicePrincipalName = serviceName + '/' + hostName + (serviceRealm == null ? "" : '@' + serviceRealm);

            IWindowsCredentialsHandle clientCredentials = WindowsCredentialsHandleImpl.getCurrent(SECURITY_PACKAGE);
            clientContext = new WindowsSecurityContextImpl();
            clientContext.setPrincipalName(authorizationId);
            clientContext.setCredentialsHandle(clientCredentials);
            clientContext.setSecurityPackage(SECURITY_PACKAGE);
        }

        @Override
        public String getMechanismName() {
            return "GSSAPI";
        }

        @Override
        public boolean hasInitialResponse() {
            return true;
        }

        @Override
        public byte[] evaluateChallenge(final byte[] challenge) throws SaslException {
            notNull("challenge", challenge);
            isTrueArgument("challenge length > 0", challenge.length > 0);

            SecBufferDesc continueToken = new SecBufferDesc(SECBUFFER_TOKEN, challenge);
            clientContext.initialize(clientContext.getHandle(), continueToken, servicePrincipalName);
            return clientContext.getToken();
        }

        @Override
        public boolean isComplete() {
            return !clientContext.isContinue();
        }

        @Override
        public byte[] unwrap(final byte[] incoming, final int offset, final int len) throws SaslException {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public byte[] wrap(final byte[] outgoing, final int offset, final int len) throws SaslException {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public Object getNegotiatedProperty(final String propName) {
            throw new UnsupportedOperationException("Not implemented yet!");
        }

        @Override
        public void dispose() throws SaslException {
            clientContext.dispose();
        }
    }
}
