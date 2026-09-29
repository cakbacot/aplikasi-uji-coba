package kurofin.controller;

import kurofin.dao.DatabaseHelper;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller untuk pemeliharaan basis data: Backup dan Restore offline (FR-21 s/d FR-23).
 */
public class DatabaseController {

    private final List<Runnable> restoreListeners = new ArrayList<>();

    public void addRestoreListener(Runnable listener) {
        if (listener != null) {
            restoreListeners.add(listener);
        }
    }

    public void notifyDatabaseRestored() {
        for (Runnable r : restoreListeners) {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Backup database kurofin.db ke file tujuan yang dipilih pengguna.
     */
    public void backupDatabase(File targetFile) throws IOException, SQLException {
        if (targetFile == null) {
            throw new IllegalArgumentException("File tujuan backup tidak boleh kosong.");
        }
        if (!targetFile.getName().toLowerCase().endsWith(".db")) {
            targetFile = new File(targetFile.getParentFile(), targetFile.getName() + ".db");
        }
        DatabaseHelper.backupDatabase(targetFile);
    }

    /**
     * Restore database dari file backup yang dipilih pengguna.
     */
    public void restoreDatabase(File sourceFile) throws IOException, SQLException {
        if (sourceFile == null || !sourceFile.exists()) {
            throw new IllegalArgumentException("File sumber restore tidak ditemukan.");
        }
        DatabaseHelper.restoreDatabase(sourceFile);
        notifyDatabaseRestored();
    }

    public File getActiveDatabaseFile() {
        return DatabaseHelper.getDatabaseFile();
    }
}
