package cl.uct.ojociudadano.data.media;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Proveedor de prueba que entrega una imagen sin informar su tamaño.
 *
 * Permite comprobar que la importación aplica el límite mientras copia,
 * incluso cuando OpenableColumns.SIZE no está disponible.
 */
public class UnknownSizeImageProvider extends ContentProvider {

    public static final String AUTHORITY = "cl.uct.ojociudadano.test.images";
    public static final int CONTENT_SIZE_BYTES = 1024;

    private static final String SOURCE_FILE_NAME = "unknown_size_image.jpg";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = sourceFile();

        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            try (FileOutputStream output = new FileOutputStream(file)) {
                output.write(new byte[CONTENT_SIZE_BYTES]);
            } catch (IOException exception) {
                throw new FileNotFoundException(exception.getMessage());
            }
        }

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public String getType(Uri uri) {
        return "image/jpeg";
    }

    @Override
    public Cursor query(
            Uri uri,
            String[] projection,
            String selection,
            String[] selectionArgs,
            String sortOrder
    ) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private File sourceFile() {
        return new File(getContext().getCacheDir(), SOURCE_FILE_NAME);
    }
}
