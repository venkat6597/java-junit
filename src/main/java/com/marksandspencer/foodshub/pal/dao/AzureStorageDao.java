package com.marksandspencer.foodshub.pal.dao;

import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.microsoft.azure.storage.blob.CloudBlockBlob;

import java.io.File;
import java.util.Map;

public interface AzureStorageDao {
    CloudBlockBlob fetchDownloadTemplate(String downloadTemplateName);

    String writeNotificationsBlob(Map<String, Object> message) throws PALServiceException;

    File downloadBlobToFile(String blobFileName, File tempFile);
}
