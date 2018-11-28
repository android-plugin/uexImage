package org.zywx.wbpalmstar.plugin.ueximage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Environment;

import org.json.JSONException;
import org.json.JSONObject;
import org.zywx.wbpalmstar.plugin.ueximage.util.Constants;
import org.zywx.wbpalmstar.plugin.ueximage.util.UEXImageUtil;
import org.zywx.wbpalmstar.plugin.ueximage.vo.CompressImageVO;
import org.zywx.wbpalmstar.plugin.ueximage.vo.PicSizeVO;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

public class ImageAgent {

    private ImageAgent() {
    }

    public static ImageAgent getInstance() {
        return ImageAgentHolder.sInstance;
    }

    private static class ImageAgentHolder {
        private static final ImageAgent sInstance = new ImageAgent();
    }

    public void compressImage(final Context context, final EUExImage mEuExImage,
            final CompressImageVO mCompressImageVO) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                String status = Constants.JK_OK;
                int errorCode = Constants.ERROR_CODE_SUCCESS;
                String srcPath = mCompressImageVO.getSrcPath();

                String desPath = UEXImageUtil.getImageCacheDir(context)
                        + File.separator + Constants.COMPRESS_TEMP_FILE_PREFIX
                        + new Date().getTime() + "."
                        + Constants.COMPRESS_TEMP_FILE_SUFFIX;
                new File(desPath);
                int desLength = mCompressImageVO.getDesLength();

                PicSizeVO mPicSizeVO = UEXImageUtil.getPicSizeVOList(desLength);
                int length = desLength - 2 * 1024;
                float desH = mPicSizeVO.height;
                float desW = mPicSizeVO.width;
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                Bitmap bitmap = BitmapFactory.decodeFile(srcPath, opts);
                opts.inJustDecodeBounds = false;
                int picW = opts.outWidth;
                int picH = opts.outHeight;
                int size = 0;
                if (picW <= desW && picH <= desH) {
                    size = 1;
                } else {
                    double scale;
                    if(desH>desW) {
                        scale = (picW >= picH) ? (picW / desH)
                                : (picH / desH);
                    }else {
                        scale = (picW >= picH) ? (picW / desW)
                                : (picH / desW);
                    }
//                    scale = (picW >= picH) ? (picW / desW)
//                                : (picH / desH);
                    size = UEXImageUtil.getInSampleSize(scale);
                }
                opts.inSampleSize = size;
                bitmap = BitmapFactory.decodeFile(srcPath, opts);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int quality = 100;
                JSONObject cbJson = new JSONObject();
                try {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
                    int fileLength = baos.toByteArray().length;
                    while (true) {
                        while ((fileLength > length) && (quality > 40)) {
                            baos.reset();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, quality,
                                    baos);
                            quality -= 20;
                            fileLength = baos.toByteArray().length;
                        }
                        if (fileLength > length) {
                            baos.writeTo(new FileOutputStream(desPath));
                            opts = new BitmapFactory.Options();
                            opts.inJustDecodeBounds = true;
                            bitmap = BitmapFactory.decodeFile(desPath, opts);
                            opts.inJustDecodeBounds = false;
                            opts.inSampleSize = UEXImageUtil
                                    .getInSampleSize(fileLength / desLength);
                            bitmap = BitmapFactory.decodeFile(desPath, opts);
                            baos.reset();
                            bitmap.compress(Bitmap.CompressFormat.JPEG, quality,
                                    baos);
                            fileLength = baos.toByteArray().length;
                            quality = 100;
                        } else {
                            break;
                        }
                }
                    baos.writeTo(new FileOutputStream(desPath));
                    cbJson.put(Constants.JK_FILE_PATH, desPath);
                } catch (Exception e) {
                    status = Constants.JK_FAIL;
                    errorCode = Constants.ERROR_CODE_FAIL;
                    e.printStackTrace();
                } finally {
                    try {
                        baos.flush();
                        baos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                }
            }
            try {
                    cbJson.put(Constants.JK_STATUSE, status);
                } catch (JSONException e) {
                e.printStackTrace();
            }
                mEuExImage.cbCompressImage(cbJson, errorCode);
                return null;
            }
        }.execute();
    }

    public void clearOutputImages(Context context) {
        delDir(UEXImageUtil.getImageCacheDir(context));
        delDir(Environment.getExternalStorageDirectory().getAbsolutePath()
                + File.separator + Constants.TEMP_PATH);
    }

    private void delDir(String filePath) {
        File directory = new File(filePath);
        if (directory.exists()) {
            for (File file : directory.listFiles()) {
                if (!Constants.NO_MEDIA.equals(file.getName())) {
                    file.delete();
                }
            }
        }
    }

}
