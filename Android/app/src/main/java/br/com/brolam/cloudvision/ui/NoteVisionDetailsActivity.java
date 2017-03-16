/*
 * Copyright (C) 2017 Breno Marques
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.brolam.cloudvision.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.HashMap;
import br.com.brolam.cloudvision.R;
import br.com.brolam.cloudvision.data.CloudVisionProvider;
import br.com.brolam.cloudvision.data.models.NoteVision;
import br.com.brolam.cloudvision.data.models.NoteVisionItem;
import br.com.brolam.cloudvision.ui.adapters.NoteVisionDetailsAdapter;
import br.com.brolam.cloudvision.ui.adapters.holders.NoteVisionDetailsHolder;
import br.com.brolam.cloudvision.ui.helpers.ActivityHelper;
import br.com.brolam.cloudvision.ui.helpers.AppAnalyticsHelper;
import br.com.brolam.cloudvision.ui.helpers.ClipboardHelper;
import br.com.brolam.cloudvision.ui.helpers.FormatHelper;
import br.com.brolam.cloudvision.ui.helpers.ImagesHelper;
import br.com.brolam.cloudvision.ui.helpers.LoginHelper;
import br.com.brolam.cloudvision.ui.helpers.ShareHelper;

/**
 * Manutenção e exbição de um Note Vision Item.
 * @author Breno Marques
 * @version 1.00
 * @since Release 01
 */
public class NoteVisionDetailsActivity extends ActivityHelper implements LoginHelper.ILoginHelper, NoteVisionDetailsAdapter.INoteVisionDetailsAdapter, View.OnClickListener, ValueEventListener {
    private static final String TAG = "DetailsActivity";
    public static final String NOTE_VISION_KEY = "noteVisionKey";
    public static final String NOTE_VISION = "noteVision";
    private static final int NOTE_VISION_REQUEST_COD = 1000;

    private String noteVisionKey;
    private HashMap noteVision;

    FloatingActionButton fab;
    RecyclerView recyclerView;
    LinearLayoutManager linearLayoutManager;

    LoginHelper loginHelper;
    CloudVisionProvider cloudVisionProvider;
    ImagesHelper imagesHelper;
    NoteVisionDetailsAdapter noteVisionDetailsAdapter;
    AppAnalyticsHelper appAnalyticsHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_vision_details);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        this.recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        setSupportActionBar(toolbar);
        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        this.linearLayoutManager = new LinearLayoutManager(this);
        this.linearLayoutManager.setReverseLayout(true);
        this.linearLayoutManager.setStackFromEnd(true);
        this.recyclerView.setLayoutManager(this.linearLayoutManager);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(this);
        setSaveInstanceState(savedInstanceState);
         /*
         * Criar um LoginHelper para registrar o login do usuário no aplicativo.
         * Veja os métodos onResume, onPause e onActivityResult para mais detalhes
         * sobre o fluxo de registro do usuário.
         */
        this.loginHelper = new LoginHelper(this, null, this);
    }

    /**
     * Recuperar um Intent com os parâmetros necessário para exibir os detalhes de um Note Vision.
     * @param context      informar a atividade que receberá o retorno.
     * @param noteVisionKey informar uma chave válida.
     * @param noteVision    informar um NoteVision válido.
     */
    public static Intent getIntent(Context context, String noteVisionKey, HashMap noteVision) {
        Intent intent = new Intent(context, NoteVisionDetailsActivity.class);
        intent.putExtra(NOTE_VISION_KEY, noteVisionKey);
        intent.putExtra(NOTE_VISION, noteVision);
        return intent;
    }

    /**
     * Acionar a exibição dos detalhes de um Note Vision.
     * @param activity      informar a atividade que receberá o retorno.
     * @param requestCod    informar o código de requisição dessa atividade.
     * @param noteVisionKey informar uma chave válida.
     * @param noteVision    informar um NoteVision válido.
     */
    public static void show(Activity activity, int requestCod, String noteVisionKey, HashMap noteVision) {
        Intent intent = getIntent(activity, noteVisionKey, noteVision);
        activity.startActivityForResult(intent, requestCod);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(NOTE_VISION_KEY, this.noteVisionKey);
        outState.putSerializable(NOTE_VISION, this.noteVision);
        super.saveCoordinatorLayoutHelper(outState);
        super.saveNestedScrollHelperViewState(outState);
        super.saveRecyclerViewState(outState);

    }

    private void setSaveInstanceState(Bundle savedInstanceState) {
        // read parameters from the  savedInstanceState ou intent used to launch the activity.
        Bundle bundle = savedInstanceState != null ? savedInstanceState : getIntent().getExtras();
        if (bundle != null) {
            this.noteVisionKey = bundle.getString(NOTE_VISION_KEY);
            this.noteVision = (HashMap) bundle.getSerializable(NOTE_VISION);
            this.setHeader();
        }
    }

    /**
     * Atualizar o cabeçalho com informações do Note Vision.
     */
    private void setHeader() {
        //this.setTitle(NoteVision.getTitle(this.noteVision));
        ActionBar actionBar = this.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(NoteVision.getTitle(this.noteVision));
            actionBar.setSubtitle(FormatHelper.getDateCreated(this, NoteVision.getCreated(this.noteVision)));
        }
        ImageView imageViewBackground = (ImageView) this.findViewById(R.id.imageViewBackground);
        if ((this.imagesHelper != null) && (imageViewBackground != null)) {
            this.imagesHelper.loadNoteVisionBackground(this.noteVisionKey, this.noteVision, imageViewBackground);
        }
    }

    @Override
    public void onBackPressed() {

        if ( this.noteVisionKey != null){
            Intent data = new Intent();
            data.putExtra(NOTE_VISION_KEY, this.noteVisionKey);
            setResult(0,data);
            this.finish();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        this.loginHelper.begin();
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.loginHelper.pause();
        if ( this.cloudVisionProvider != null){
            this.cloudVisionProvider.removeListenerOneNoteVision(this.noteVisionKey, this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if ( this.cloudVisionProvider != null){
            this.cloudVisionProvider.removeListenerOneNoteVision(this.noteVisionKey, this);
        }
    }

    @Override
    public void onLogin(FirebaseUser firebaseUser) {
        if ( this.cloudVisionProvider == null) {
            this.cloudVisionProvider = new CloudVisionProvider(firebaseUser.getUid());
            this.imagesHelper = new ImagesHelper(this, this.cloudVisionProvider);
            this.noteVisionDetailsAdapter = new NoteVisionDetailsAdapter(
                    HashMap.class,
                    R.layout.holder_note_vision_details,
                    NoteVisionDetailsHolder.class,
                    this.cloudVisionProvider.getQueryNoteVisionItems(this.noteVisionKey)
            );
            this.noteVisionDetailsAdapter.setINoteVisionDetailsAdapter(this);
            this.recyclerView.setAdapter(noteVisionDetailsAdapter);
            this.appAnalyticsHelper = new AppAnalyticsHelper(this);
        }
        this.cloudVisionProvider.addListenerOneNoteVision(this.noteVisionKey, this);
    }

    @Override
    public void onDataChange(DataSnapshot dataSnapshot) {
        this.noteVision = (HashMap)dataSnapshot.getValue();
        setHeader();
    }

    @Override
    public void onCancelled(DatabaseError databaseError) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note_vision_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.note_vision_share:
                ShareHelper.noteVision(this, noteVision);
                this.appAnalyticsHelper.logNoteVisionShared(TAG);
                return true;
            case R.id.note_vision_background:
                addNoteVisionBackground(noteVisionKey, noteVision);
                return true;
            //Excluir um Note Vision.
            case R.id.note_vision_delete:
                this.deleteNoteVision();
                this.appAnalyticsHelper.logNoteVisionDeleted(TAG);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        /**
         * Validar se o login do usuário foi realizado com sucess.
         * Sendo importante destacar, se o login for cancelado a atividade será encerrada!
         */
        if (loginHelper.checkLogin(requestCode, resultCode)) {
            //Verificar se exitem algum requisão para salvar uma imagem.
            //Observação: Se ocorrer a rotação da tela essa verificação será cancelada,
            //            sendo assim, essa verificar também deve ocorrer novamente na
            //            reconstrução da tela, {@link ImagesHelper.restoreStorageReference }
            if (this.imagesHelper != null) {
                this.imagesHelper.onActivityResult(requestCode, resultCode, data);
            }
            String noteVisionItemKey = data != null ? data.getStringExtra(NoteVisionActivity.NOTE_VISION_ITEM_KEY) : null;
            setItemSelectedKey(noteVisionItemKey);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            //Adicionar um Note Vision Item.
            case R.id.fab:
                String title = NoteVision.getTitle(this.noteVision);
                NoteVisionActivity.addNoteVisionContent(this, NOTE_VISION_REQUEST_COD, this.noteVisionKey, title, false);
                return;
        }
    }

    @Override
    public void onNoteVisionItemButtonClick(String noteVisionItemKey, HashMap noteVisionItem, View imageButton) {
        switch (imageButton.getId()) {
            //Editar o Note Vision Seleciondo.
            case R.id.imageButtonEdit:
                String title = NoteVision.getTitle(this.noteVision);
                String content = NoteVisionItem.getContent(noteVisionItem);
                NoteVisionActivity.updateNoteVision(this, NOTE_VISION_REQUEST_COD, this.noteVisionKey, noteVisionItemKey, title, content, true);
                return;
            //Copiar para a área de transferência o Note Vision Item selecionado.
            case R.id.imageButtonCopy:
                ClipboardHelper clipboardHelper = new ClipboardHelper(this);
                clipboardHelper.noteVisionItem(noteVision, noteVisionItem);
                this.appAnalyticsHelper.logNoteVisionCopyToClipboard(TAG);
                Toast.makeText(this, R.string.note_vision_clipboard_copied, Toast.LENGTH_LONG).show();
                return;
            //Compartilhar o Note Vision Item selelcionado com outros aplicativos.
            case R.id.imageButtonShared:
                ShareHelper.noteVisionItem(this, noteVision, noteVisionItem);
                this.appAnalyticsHelper.logNoteVisionShared(TAG);
                return;
            //Excluir um Note Vsion Item selecionado.
            case R.id.imageButtonDelete:
                deleteNoteVisionItem(noteVisionItemKey);
                return;
        }
    }

    @Override
    public int getContentWidth() {
        return this.recyclerView.getWidth();
    }

    /**
     * Solicitar a confirmação de exclusão de um Note Vision Item.
     * @param noteVisionItemKey informar uma chave válida.
     */
    private void deleteNoteVisionItem(final String noteVisionItemKey) {
        Snackbar snackbar = Snackbar.make
                (
                        this.fab,
                        String.format(getString(R.string.note_vision_confirm_delete), getString(R.string.ok)),
                        BaseTransientBottomBar.LENGTH_LONG
                );
        snackbar.setAction(R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cloudVisionProvider != null) {
                    cloudVisionProvider.deleteNoteVisionItem(noteVisionKey, noteVisionItemKey);
                }
            }
        });
        snackbar.show();
    }


    /**
     * Adicionar a imagem de background ao NotVision e também verificar a permissão a câmera
     * fotográfica.
     * @param noteVisionKey informar uma chave válida
     * @param noteVision informar um NoteVision válido.
     */
    private void addNoteVisionBackground(String noteVisionKey, HashMap noteVision){
        // Check for the camera permission before accessing the camera.  If the
        // permission is not granted yet, request permission.
        int rc = ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        if (rc == PackageManager.PERMISSION_GRANTED) {
            if (this.imagesHelper  != null){
                try {
                    NoteVision.BackgroundOrigin backgroundOrigin = NoteVision.getBackground(noteVision);
                    if ( backgroundOrigin == NoteVision.BackgroundOrigin.LOCAL) {
                        Toast.makeText(this, R.string.note_vision_alert_background_image_in_processing, Toast.LENGTH_LONG).show();
                    } else {
                        this.imagesHelper.takeNoteVisionBackground(noteVisionKey);
                        this.appAnalyticsHelper.logNoteVisionAddBackground(TAG);
                    }
                } catch (IOException e) {
                    Log.e(TAG, e.getMessage());
                    Toast.makeText(this, String.format(getString(R.string.main_activity_request_error),ImagesHelper.REQUEST_IMAGE_CAPTURE), Toast.LENGTH_LONG).show();
                }
            }

        } else {
            ActivityHelper.requestCameraPermission(TAG, this, fab);
        }

    }

    /**
     * Solicitar a confirmação de exclusão de um Note Vision.
     */
    private void deleteNoteVision() {
        Snackbar snackbar = Snackbar.make
                (
                        this.fab,
                        String.format(getString(R.string.note_vision_confirm_delete), getString(R.string.ok)),
                        BaseTransientBottomBar.LENGTH_LONG
                );
        snackbar.setAction(R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (cloudVisionProvider != null) {
                    cloudVisionProvider.removeListenerOneNoteVision(noteVisionKey, NoteVisionDetailsActivity.this);
                    cloudVisionProvider.deleteNoteVision(noteVisionKey);
                    appAnalyticsHelper.logNoteVisionDeleted(TAG);
                    NoteVisionDetailsActivity.this.finish();
                }
            }
        });
        snackbar.show();
    }

    @Override
    public void restoreViewState() {
        super.restoreCoordinatorLayoutHelperViewState();
        super.restoreNestedScrollHelperViewState();
        super.restoreRecyclerViewState();
        if (getItemSelectedKey() != null) {
            int itemPosition = this.noteVisionDetailsAdapter.getItemPosition(getItemSelectedKey());
            this.recyclerView.getLayoutManager().scrollToPosition(itemPosition);
            clearItemSelectedKey();
        }
    }
}
