/**
 * Copyright Google Inc. All Rights Reserved.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.firebase.udacity.friendlychat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.BuildConfig;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    public static final String ANONYMOUS = "anonymous";
    public static final String FRIENDLY_MSG_LENGTH_KEY = "friendly_msg_length";
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final int RC_SIGN_IN = 1;
    private static final int RC_PHOTO_PICKER = 2;

    //private ListView mMessageListView;
    //private MessageAdapter mMessageAdapter;
    private FirebaseRecyclerAdapter mFirebaseRecyclerAdapter;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;

    private String mUsername;

    // firebase realtime database objects
    private FirebaseDatabase mFirebaseDB;
    private DatabaseReference mMessagesDBReference;
    private ChildEventListener mChildEventListener;
    // firebase storage
    private FirebaseStorage mFbStorage;
    private StorageReference mChatPhotoStorageReference;
    // firebase remote config
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    // firebase auth
    private FirebaseAuth mFbAuth;
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    // recycler view
    private RecyclerView mRecyclerViewMessages;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mUsername = ANONYMOUS;

        // intialize firebase objects
        // for database
        mFirebaseDB = FirebaseDatabase.getInstance();
        // for remote config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        // for auth
        mFbAuth = FirebaseAuth.getInstance();
        // for storage
        mFbStorage = FirebaseStorage.getInstance();

        // recycler view
        mRecyclerViewMessages = (RecyclerView) findViewById(R.id.rvMessages);
        mRecyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));

        mMessagesDBReference = mFirebaseDB.getReference().child("messages");
        mChatPhotoStorageReference = mFbStorage.getReference().child("chat_photos");

        mFirebaseRecyclerAdapter = new FirebaseRecyclerAdapter<FriendlyMessage, MessageHolder>
                (FriendlyMessage.class, R.layout.item_message, MessageHolder.class, mMessagesDBReference) {
            @Override
            protected void populateViewHolder(MessageHolder viewHolder, FriendlyMessage model, int position) {
                if(model.getPhotoUrl()!= null) {
                    viewHolder.setPhoto(model.getPhotoUrl());
                } else {
                    viewHolder.setMessage(model.getText());
                }
                viewHolder.setName(model.getName());
            }
        };

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        //mMessageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);

        // Initialize message ListView and its adapter
        //List<FriendlyMessage> friendlyMessages = new ArrayList<>();
        //mMessageAdapter = new MessageAdapter(this, R.layout.item_message, friendlyMessages);

        mRecyclerViewMessages.setAdapter(mFirebaseRecyclerAdapter);
        //mMessageListView.setAdapter(mMessageAdapter);

        // Initialize progress bar
        mProgressBar.setVisibility(ProgressBar.INVISIBLE);

        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/jpeg");
                i.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(i, "Complete action using"),
                        RC_PHOTO_PICKER);
            }
        });

        // Enable Send button when there's text to send
        mMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        // setting the auth state listener
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                // user is signed in
                if (user != null) {
                    // pass in the user name
                    onSignedInInitialize(user.getDisplayName());
                // user is not signed in
                } else {
                    onSignedOutCleanup();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()
                                    .setIsSmartLockEnabled(false)
                                    .setAvailableProviders(
                                            Arrays.asList(new AuthUI.IdpConfig.Builder(AuthUI.EMAIL_PROVIDER).build(),
                                                    new AuthUI.IdpConfig.Builder(AuthUI.GOOGLE_PROVIDER).build()))
                                    .build(),
                            RC_SIGN_IN);
                }
            }
        };

        // firebase enabling developer mode on device
        FirebaseRemoteConfigSettings rcSettings = new FirebaseRemoteConfigSettings.Builder()
                .setDeveloperModeEnabled(BuildConfig.DEBUG)
                .build();
        mFirebaseRemoteConfig.setConfigSettings(rcSettings);
        // create a map to map all the configuration
        Map<String, Object> defaultConfigMap = new HashMap<>();
        defaultConfigMap.put(FRIENDLY_MSG_LENGTH_KEY, DEFAULT_MSG_LENGTH_LIMIT);
        mFirebaseRemoteConfig.setDefaults(defaultConfigMap);
        // custom method to fetch remote config
        fetchConfig();

        mMessageEditText.setFilters(new InputFilter[]{new InputFilter.LengthFilter(DEFAULT_MSG_LENGTH_LIMIT)});

        // Send button sends a message and clears the EditText
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FriendlyMessage fm = new FriendlyMessage(mMessageEditText.getText().toString(),
                        mUsername, null);
                mMessagesDBReference.push().setValue(fm);
                // Clear input box
                mMessageEditText.setText("");
            }
        });
    }

    private void fetchConfig() {
        // set cache expiration
        long cacheExpiration = 3600;
        // if device is in developer mode, get the latest from firebase remote config
        if(mFirebaseRemoteConfig.getInfo().getConfigSettings().isDeveloperModeEnabled()) {
            cacheExpiration = 0;
        }
        mFirebaseRemoteConfig.fetch(cacheExpiration)
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    mFirebaseRemoteConfig.activateFetched();
                    applyRetrievedLengthLimit();
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Error fetching config", e);
                    applyRetrievedLengthLimit();
                }
        });
    }

    private void applyRetrievedLengthLimit() {
        Long friendly_msg_length = mFirebaseRemoteConfig.getLong(FRIENDLY_MSG_LENGTH_KEY);
        mMessageEditText.setFilters(new InputFilter[] {
                        new InputFilter.LengthFilter(friendly_msg_length.intValue())
        });
        Log.d(TAG, FRIENDLY_MSG_LENGTH_KEY + " = " + friendly_msg_length);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Signed in!", Toast.LENGTH_SHORT).show();
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        else if (requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            // get the selected img data
            Uri selectedImageUri = data.getData();
            // get the filename for reference to store at chat_photos/<FILENAME>
            StorageReference photoRef = mChatPhotoStorageReference.child(
                    selectedImageUri.getLastPathSegment());
            // upload file to firebaes storage
            photoRef.putFile(selectedImageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    FriendlyMessage fm = new FriendlyMessage(null, mUsername, downloadUrl.toString());
                    mMessagesDBReference.push().setValue(fm);
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.sign_out_menu:
                // signing out
                AuthUI.getInstance().signOut(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFbAuth.addAuthStateListener(mAuthStateListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(mAuthStateListener != null) {
            mFbAuth.removeAuthStateListener(mAuthStateListener);
        }
        detachDatabaseReadListener();
        //mMessageAdapter.clear();
        mFirebaseRecyclerAdapter.cleanup();
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanup() {
        // take down username
        mUsername = ANONYMOUS;
        // clear the messsages
        //mMessageAdapter.clear();
        mFirebaseRecyclerAdapter.cleanup();
        // detach read listener
        detachDatabaseReadListener();
    }

    private void attachDatabaseReadListener() {
        if(mChildEventListener == null) {
            // firebase realtime database listener
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage fm = dataSnapshot.getValue(FriendlyMessage.class);
                    //mMessageAdapter.add(fm);
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    FriendlyMessage fm = dataSnapshot.getValue(FriendlyMessage.class);
                    //mMessageAdapter.add(fm);
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) { }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) { }

                @Override
                public void onCancelled(DatabaseError databaseError) { }
            };
            // setting the firebase database reference to the child event listener
            mMessagesDBReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        if(mChildEventListener != null) {
            mMessagesDBReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

}
