/**
 * Copyright Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

public class FriendlyMessage {

    private String mMessage;
    private String mName;
    private String mUid;
    private String photoUrl;

    public FriendlyMessage() {
    }

    public FriendlyMessage(String message, String name, String photoUrl) {
        this.mMessage = message;
        this.mName = name;
        this.photoUrl = photoUrl;
    }

    public FriendlyMessage(String message, String name, String uid, String photoUrl) {
        this.mMessage = message;
        this.mName = name;
        this.mUid = uid;
        this.photoUrl = photoUrl;
    }

    public String getText() {
        return mMessage;
    }

    public void setText(String message) {
        this.mMessage = message;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getUid() { return mUid; }

    public void setUid(String uid) { this.mUid = uid; }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }
}
