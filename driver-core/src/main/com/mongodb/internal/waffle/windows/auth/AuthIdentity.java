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

package com.mongodb.internal.waffle.windows.auth;

import com.sun.jna.Structure;
import com.sun.jna.WString;

import java.util.Arrays;
import java.util.List;

import static com.sun.jna.platform.win32.WTypes.*;

public class AuthIdentity extends Structure {
    // CHECKSTYLE: OFF
    public WString userName;
    public long userNameLength;
    public WString domain;
    public long domainLength;
    public WString password;
    public long passwordLength;
    public long flags = 0x02;  // UNICODE
    // CHECKSTYLE: ON

    public AuthIdentity(final String userName, final String password) {
        this.userName = new WString(userName);
        this.userNameLength = userName.length();
        this.password = new WString(password);
        this.passwordLength = password.length();
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("userName", "userNameLength", "domain", "domainLength", "password", "passwordLength", "flags");
    }
}
