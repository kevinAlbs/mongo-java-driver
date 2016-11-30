/*
 * Copyright 2008-2016 MongoDB, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.connection;

import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.MongoSecurityException;
import com.mongodb.ServerAddress;
import org.ietf.jgss.GSSCredential;
import org.ietf.jgss.GSSException;
import org.ietf.jgss.GSSManager;
import org.ietf.jgss.GSSName;
import org.ietf.jgss.Oid;

import javax.security.sasl.Sasl;
import javax.security.sasl.SaslClient;
import javax.security.sasl.SaslException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import static com.mongodb.AuthenticationMechanism.GSSAPI;
import static com.mongodb.MongoCredential.CANONICALIZE_HOST_NAME_KEY;
import static com.mongodb.MongoCredential.SERVICE_REALM_KEY;
import static com.mongodb.MongoCredential.JAVA_GSSAPI_USE_NATIVE_SSPI_KEY;
import static com.mongodb.MongoCredential.JAVA_SASL_CLIENT_PROPERTIES_KEY;
import static com.mongodb.MongoCredential.SERVICE_NAME_KEY;

class GSSAPIAuthenticator extends SaslAuthenticator {
    private static final String GSSAPI_MECHANISM_NAME = "GSSAPI";
    private static final String GSSAPI_OID = "1.2.840.113554.1.2.2";
    public static final String SERVICE_NAME_DEFAULT_VALUE = "mongodb";
    public static final Boolean CANONICALIZE_HOST_NAME_DEFAULT_VALUE = false;

    GSSAPIAuthenticator(final MongoCredential credential) {
        super(credential);

        if (getCredential().getAuthenticationMechanism() != GSSAPI) {
            throw new MongoException("Incorrect mechanism: " + this.getCredential().getMechanism());
        }
    }

    @Override
    public String getMechanismName() {
        return GSSAPI_MECHANISM_NAME;
    }

    @Override
    protected SaslClient createSaslClient(final ServerAddress serverAddress) {
        MongoCredential credential = getCredential();
        try {
            SaslClient saslClient;
            if (Boolean.valueOf(credential.getMechanismProperty(JAVA_GSSAPI_USE_NATIVE_SSPI_KEY, "false"))) {  // TODO: handle boolean type
                saslClient = WindowsGSSAPI.createSaslClient(credential.getUserName(), credential.getPassword(),
                        credential.getMechanismProperty(SERVICE_NAME_KEY, SERVICE_NAME_DEFAULT_VALUE),
                        getHostName(serverAddress),
                        credential.getMechanismProperty(SERVICE_REALM_KEY, (String) null));
            }
            else {

                // TODO: log warning if credential has a password

                Map<String, Object> saslClientProperties = getCredential().getMechanismProperty(JAVA_SASL_CLIENT_PROPERTIES_KEY, null);
                if (saslClientProperties == null) {
                    saslClientProperties = new HashMap<String, Object>();
                    saslClientProperties.put(Sasl.CREDENTIALS, getGSSCredential(credential.getUserName()));
                }

                saslClient = Sasl.createSaslClient(new String[]{GSSAPI.getMechanismName()}, credential.getUserName(),
                        credential.getMechanismProperty(SERVICE_NAME_KEY, SERVICE_NAME_DEFAULT_VALUE),
                        getHostName(serverAddress), saslClientProperties, null);
            }
            if (saslClient == null) {
                throw new MongoSecurityException(credential, String.format("No platform support for %s mechanism", GSSAPI));
            }

            return saslClient;
        } catch (SaslException e) {
            throw new MongoSecurityException(credential, "Exception initializing SASL client", e);
        } catch (GSSException e) {
            throw new MongoSecurityException(credential, "Exception initializing GSSAPI credentials", e);
        } catch (UnknownHostException e) {
            throw new MongoSecurityException(credential, "Unable to canonicalize host name + " + serverAddress);
        }
    }

    private GSSCredential getGSSCredential(final String userName) throws GSSException {
        Oid krb5Mechanism = new Oid(GSSAPI_OID);
        GSSManager manager = GSSManager.getInstance();
        GSSName name = manager.createName(userName, GSSName.NT_USER_NAME);
        return manager.createCredential(name, GSSCredential.INDEFINITE_LIFETIME, krb5Mechanism, GSSCredential.INITIATE_ONLY);
    }

    private String getHostName(final ServerAddress serverAddress) throws UnknownHostException {
        return getCredential().getMechanismProperty(CANONICALIZE_HOST_NAME_KEY, CANONICALIZE_HOST_NAME_DEFAULT_VALUE)
               ? InetAddress.getByName(serverAddress.getHost()).getCanonicalHostName()
               : serverAddress.getHost();
    }
}
