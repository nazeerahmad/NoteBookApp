package com.example.notebookapp.view;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notebookapp.R;
import com.example.notebookapp.network.ApiService;
import com.example.notebookapp.network.ClientApi;
import com.example.notebookapp.network.model.Note;
import com.example.notebookapp.network.model.User;
import com.example.notebookapp.utils.MyDividerItemDecoration;
import com.example.notebookapp.utils.PrefUtils;
import com.example.notebookapp.utils.RecyclerTouchListener;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.jakewharton.retrofit2.adapter.rxjava2.HttpException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static  final String TAG= MainActivity.class.getSimpleName();
    private ApiService apiService;
    private CompositeDisposable disposable=new CompositeDisposable();
    private NoteAdapter noteAdapter;
    private List<Note>noteList=new ArrayList<>();

    @BindView(R.id.coordinator_layout)
    CoordinatorLayout coordinator_Layout;
    @BindView(R.id.recycler_view)
    RecyclerView recycler_view;
    @BindView(R.id.txt_empty_notes_view)
    TextView noNoteView;

    private Context context;

    private static final int MY_REQUEST_CODE = 101;
    List<AuthUI.IdpConfig>providers;
    TextView sign_out;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(R.layout.activity_main);
        context= MainActivity.this;
        ButterKnife.bind(this);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(getString(R.string.activity_title_home));
        sign_out=(TextView)toolbar.findViewById(R.id.signOut);
        setSupportActionBar(toolbar);

        sign_out.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AuthUI.getInstance()
                        .signOut(MainActivity.this)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                sign_out.setEnabled(false);
                                showSignInOptions();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),""+e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });

        providers= Arrays.asList(new AuthUI.IdpConfig.EmailBuilder().build(),
                new  AuthUI.IdpConfig.PhoneBuilder().build(),
                new  AuthUI.IdpConfig.FacebookBuilder().build(),
                new  AuthUI.IdpConfig.GoogleBuilder().build()
        );
        showSignInOptions();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
                showNoteDialog(false,null,-1);
            }
        });

        whiteNotificationBar(fab);

        apiService= ClientApi.getRetrofit(getApplicationContext()).create(ApiService.class);

        noteAdapter=new NoteAdapter(context,noteList);

      RecyclerView.LayoutManager layoutManager=new LinearLayoutManager(getApplicationContext());
      recycler_view.setLayoutManager(layoutManager);
        recycler_view.setItemAnimator(new DefaultItemAnimator());
        recycler_view.addItemDecoration(new MyDividerItemDecoration(this, LinearLayoutManager.VERTICAL, 16));
        recycler_view.setAdapter(noteAdapter);

        recycler_view.addOnItemTouchListener(new RecyclerTouchListener(this,
                recycler_view, new RecyclerTouchListener.ClickListener() {
            @Override
            public void onClick(View view, final int position) {
            }

            @Override
            public void onLongClick(View view, int position) {
                showActionsDialog(position);
            }
        }));


        if (TextUtils.isEmpty(PrefUtils.getApiKey(this))) {
            registerUser();
        } else {
            // user is already registered, fetch all notes
            fetchAllNotes();
        }
    }

    private void showSignInOptions() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setTheme(R.style.MyTheme)
                        .build(),MY_REQUEST_CODE
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==MY_REQUEST_CODE){

            IdpResponse response = IdpResponse.fromResultIntent(data);
            if(resultCode==RESULT_OK){
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//                Toast.makeText(getApplicationContext(),""+user.getEmail(),Toast.LENGTH_SHORT).show();
                sign_out.setEnabled(true);
            }else {
                Toast.makeText(getApplicationContext(),""+response.getError().getMessage(),Toast.LENGTH_SHORT).show();

            }
        }
    }

    private void registerUser() {

        final String unique_id= UUID.randomUUID().toString();

        disposable.add(apiService
                .register(unique_id)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<User>() {
                    @Override
                    public void onSuccess(User user) {
                            PrefUtils.setApiKey(getApplicationContext(),user.getApi_key());
//                        System.out.println("suuuuuu "+unique_id );

                        Toast.makeText(getApplicationContext(),
                                "Device is registered successfully! ApiKey: " + PrefUtils.getApiKey(getApplicationContext()),
                                Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                        showError(e);
                    }
                })
        );

    }

    private void fetchAllNotes(){
        disposable.add(
                apiService.getAllNotes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .map(new Function<List<Note>, List<Note>>() {
                    @Override
                    public List<Note> apply(List<Note> notes) throws Exception {
                        Collections.sort(notes, new Comparator<Note>() {
                            @Override
                            public int compare(Note n1, Note n2) {
                                return n2.getId() - n1.getId();
                            }
                        });
                        return notes;
                    }
                })
                .subscribeWith(new DisposableSingleObserver<List<Note>>() {
                    @Override
                    public void onSuccess(List<Note> notes) {
                        noteList.clear();
                        noteList.addAll(notes);
                        noteAdapter.notifyDataSetChanged();
                        toggleEmptyNotes();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"onError "+e.getMessage());
                        showError(e);
                    }
                })
        );
    }

    private void createNote(String note){
        disposable.add(
                apiService.createNote(note)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<Note>() {
                    @Override
                    public void onSuccess(Note note) {
                        if(!TextUtils.isEmpty(note.getError())){
                            Toast.makeText(getApplicationContext(),note.getError(),Toast.LENGTH_LONG).show();
                            return;
                        }
                        Log.d(TAG,"new note created "+note.getId()+","+note.getNote()+","+note.getTimestamp());
                        noteList.add(0,note);
                        noteAdapter.notifyItemInserted(0);

                        toggleEmptyNotes();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"onError "+e.getMessage());
                        showError(e);
                    }
                })
        );
    }


    private void updateNote(int noteId,final String note, final int position){
        disposable.add(
                apiService.updatNote(noteId,note)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        Log.d(TAG,"Note Updated");
                        Note n=noteList.get(position);
                        n.setNote(note);
                        noteList.set(position,n);
                        noteAdapter.notifyItemChanged(position);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG,"onError "+e.getMessage());
                        showError(e);
                    }
                })
        );
    }

private void deleteNote(final int noteId,final int position){
        Log.e(TAG,"Delete Note "+noteId+","+position);
        disposable.add(
                apiService.deleteNote(noteId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {

                        Log.d(TAG,"Note Deleted "+noteId);

                        noteList.remove(position);
                        noteAdapter.notifyItemRemoved(position);

                        Toast.makeText(MainActivity.this, "Note deleted!", Toast.LENGTH_SHORT).show();
                        toggleEmptyNotes();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG, "onError: " + e.getMessage());
                        showError(e);
                    }
                })
        );
}

    private void toggleEmptyNotes() {
        if (noteList.size() > 0) {
            noNoteView.setVisibility(View.GONE);
        } else {
            noNoteView.setVisibility(View.VISIBLE);
        }
    }

    private void showActionsDialog(final int position) {
        CharSequence colors[] = new CharSequence[]{"Edit", "Delete"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose option");
        builder.setItems(colors, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showNoteDialog(true, noteList.get(position), position);
                } else {
                    deleteNote(noteList.get(position).getId(), position);
                }
            }
        });
        builder.show();
    }

    private void whiteNotificationBar(View view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int flags = view.getSystemUiVisibility();
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            view.setSystemUiVisibility(flags);
            getWindow().setStatusBarColor(Color.WHITE);
        }
    }

    private void showNoteDialog(final boolean shouldUpdate, final Note note, final int position) {
        LayoutInflater layoutInflater=LayoutInflater.from(getApplicationContext());
        View view=layoutInflater.inflate(R.layout.note_dialg,null);

        AlertDialog.Builder alertDialogBuilderUserInput = new AlertDialog.Builder(MainActivity.this);
        alertDialogBuilderUserInput.setView(view);

        final EditText inputNote=view.findViewById(R.id.note);
        TextView dailogTitle=view.findViewById(R.id.dialog_title);
        dailogTitle.setText(!shouldUpdate ? getString(R.string.lbl_new_note_title) : getString(R.string.lbl_edit_note_title));

        if(!shouldUpdate && note!=null ){
                inputNote.setText(note.getNote());
        }

        alertDialogBuilderUserInput
                .setCancelable(false)
                .setPositiveButton(shouldUpdate ? "update" : "save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();

                    }
                });
        final AlertDialog alertDialog=alertDialogBuilderUserInput.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show toast message when no text is entered
                if (TextUtils.isEmpty(inputNote.getText().toString())) {
                    Toast.makeText(MainActivity.this, "Enter note!", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    alertDialog.dismiss();
                }

                // check if user updating note
                if (shouldUpdate && note != null) {
                    // update note by it's id
                    updateNote(note.getId(), inputNote.getText().toString(), position);
                } else {
                    // create new note
                    createNote(inputNote.getText().toString());
                }
            }
        });


    }




    private void showError(Throwable e) {
        String message = "";
        try {
            if (e instanceof IOException) {
                message = "No internet connection!";
            } else if (e instanceof HttpException) {
                HttpException error = (HttpException) e;
                String errorBody = error.response().errorBody().string();
                JSONObject jObj = new JSONObject(errorBody);

                message = jObj.getString("error");
            }
        } catch (IOException e1) {
            e1.printStackTrace();
        } catch (JSONException e1) {
            e1.printStackTrace();
        } catch (Exception e1) {
            e1.printStackTrace();
        }

        if (TextUtils.isEmpty(message)) {
            message = "Unknown error occurred! Check LogCat.";
        }

        Snackbar snackbar = Snackbar
                .make(coordinator_Layout, message, Snackbar.LENGTH_LONG);

        View sbView = snackbar.getView();
        TextView textView = sbView.findViewById(R.id.snackbar_text);
        textView.setTextColor(Color.YELLOW);
        snackbar.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }
}
