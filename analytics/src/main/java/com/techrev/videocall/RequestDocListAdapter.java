package com.techrev.videocall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.gson.Gson;


import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class RequestDocListAdapter extends RecyclerView.Adapter<RequestDocListAdapter.MyViewHolder> {

    private static final String TAG = "RequestDocListAdapter";
    private Activity mActivity;
    private List<RequestDocModel.RequestDocuments> mList;
    private String authToken, requestID;
    static RetrofitNetworkClass networkClass = new RetrofitNetworkClass();
    static Retrofit retrofitLocal = networkClass.callingURL();
    static NetworkInterface serviceLocal = retrofitLocal.create(NetworkInterface.class);

    public interface OnDeleteDocument {
        public abstract void onDeleteCompleted();
    }

    private OnDeleteDocument onDeleteDocument;

    public RequestDocListAdapter (Activity activity , List<RequestDocModel.RequestDocuments> list, String auth, OnDeleteDocument deleteDocument){
        this.mActivity = activity;
        this.mList = list;
        this.authToken = auth;
        this.onDeleteDocument = deleteDocument;
        Log.d(TAG, "RequestDocListAdapter: "+new Gson().toJson(mList));
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.add_document_item_view, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

        Log.d(TAG, "onBindViewHolder: POSITION: "+position+" & DOC NAME: "+mList.get(position).getDocumentName());
        MyViewHolder viewHolder = holder;
        viewHolder.tv_document_title.setVisibility(View.VISIBLE);
        viewHolder.cb_document_title.setVisibility(View.GONE);
        viewHolder.tv_document_title.setText(mList.get(position).getDocumentName());
        viewHolder.iv_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMoreOptionsInBottomSheet(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    private void showMoreOptionsInBottomSheet(int position) {

        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(mActivity);
        bottomSheetDialog.setContentView(R.layout.request_doc_list_more_bottom_sheet);

        TextView tv_document_name = bottomSheetDialog.findViewById(R.id.tv_document_name);
        TextView tv_view_document = bottomSheetDialog.findViewById(R.id.tv_view_document);
        TextView tv_delete_document = bottomSheetDialog.findViewById(R.id.tv_delete_document);
        TextView tv_cancel = bottomSheetDialog.findViewById(R.id.tv_cancel);

        tv_document_name.setText(mList.get(position).getDocumentName()+".pdf");

        tv_view_document.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
                Intent it = new Intent(mActivity , PreviewRequestDocumentActivity.class);
                it.putExtra("DOC_ID" , mList.get(position).getDocId());
                mActivity.startActivity(it);
            }
        });

        tv_delete_document.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
                showDocumentDeleteConfirmationDialog(position);
            }
        });

        tv_cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                bottomSheetDialog.cancel();
            }
        });

        bottomSheetDialog.show();

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void showDocumentDeleteConfirmationDialog(int position) {

        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        try {
                            deleteRequestDocumentByReqDocId(position);
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        //No button clicked
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        AlertDialog dialog = builder.setMessage("Are you sure, you want to delete this document?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(mActivity.getColor(R.color.color_primary));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(mActivity.getColor(R.color.red));

    }

    private void deleteRequestDocumentByReqDocId(int position) throws JSONException {

        ProgressDialog dialog = new ProgressDialog(mActivity);
        dialog.setMessage("Please wait");
        dialog.setCancelable(false);
        dialog.show();
        JSONObject obj = new JSONObject();
        obj.put("ReqDocId" , mList.get(position).getReqDocId());
        obj.put("requestId" , mList.get(position).getRequestId());
        String data = obj.toString();
        Call<CommonModel> responseBodyCall = serviceLocal.deleteRequestDocumentByReqDocId(authToken, data);
        responseBodyCall.enqueue(new Callback<CommonModel>() {
            @Override
            public void onResponse(Call<CommonModel> call, Response<CommonModel> response) {
                if(dialog!=null){
                    dialog.dismiss();
                }

                if(response != null){
                    Log.d(TAG , "Deleted document response: \n"+new Gson().toJson(response.body()));
                    if (response.body().getStatus().equalsIgnoreCase("1")) {
                        Toast.makeText(mActivity, "Document deleted successfully!", Toast.LENGTH_SHORT).show();
                        onDeleteDocument.onDeleteCompleted();
                    } else {
                        Toast.makeText(mActivity, "Something went wrong, please try again later!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<CommonModel> call, Throwable t) {
                if(dialog!=null){
                    dialog.dismiss();
                }
                Log.d("====onResponse", "Failure" + t.toString());
            }
        });

    }

    class MyViewHolder extends RecyclerView.ViewHolder {
        private TextView tv_document_title;
        private CheckBox cb_document_title;
        private ImageView iv_more;

        public MyViewHolder(View itemView) {
            super(itemView);
            cb_document_title = itemView.findViewById(R.id.cb_document_title);
            tv_document_title = itemView.findViewById(R.id.tv_document_title);
            iv_more = itemView.findViewById(R.id.iv_more);
        }
    }

}
