package com.marksandspencer.foodshub.pal.daoImpl;

import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.dao.AzureStorageDao;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.microsoft.azure.storage.CloudStorageAccount;
import com.microsoft.azure.storage.StorageCredentials;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlobDirectory;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidKeyException;
import java.util.Map;


@Component
@Slf4j
public class AzureStorageDaoImpl implements AzureStorageDao {

    @Value("${azure.storage.account.name}")
    private String storageAccountName;

    @Value("${azure.storage.account.key}")
    private String storageAccountKey;

    @Value("${storage.account.pal.container}")
    private String palContainer;

    @Value("${storage.account.download.template.folder}")
    private String downloadTemplatesFolder;

    @Value("${storage.account.notification.failure.folder}")
    private String notificationFailureFolder;

    /**
     * getStorageConnectionString method returns storage connectionString
     * @return String storageConnectionString
     */

    public String getStorageConnectionString() {
        return new StringBuilder().append(ApplicationConstant.DEFAULT_END_POINT_URL)
                .append(ApplicationConstant.SEPERATOR).append(ApplicationConstant.ACCOUNT_NAME).append(storageAccountName.trim())
                .append(ApplicationConstant.SEPERATOR).append(ApplicationConstant.ACCOUNT_KEY).append(storageAccountKey.trim())
                .toString().trim();
    }

    /**
     * getStorageCredentials method returns storage credentials
     * @return StorageCredentials
     * @throws PALServiceException
     */
    public StorageCredentials getStorageCredentials() throws PALServiceException {
        try {
            return StorageCredentials.tryParseCredentials(getStorageConnectionString());
        } catch (StorageException | InvalidKeyException e) {
            throw new PALServiceException("Exception while retrieving storage credential", e.toString());
        }
    }

    /**
     * getCloudBlobContainer method returns cloud blob container reference
     *
     * @param containerName
     * @return CloudBlobContainer
     * @throws PALServiceException
     */
    public CloudBlobContainer getCloudBlobContainer(String containerName) throws PALServiceException {

        CloudBlobContainer container = null;
        try {
            CloudStorageAccount account = CloudStorageAccount.parse(getStorageConnectionString());
            CloudBlobClient blobClient = account.createCloudBlobClient();
            container = blobClient.getContainerReference(containerName);

        } catch (InvalidKeyException | URISyntaxException | StorageException e) {
            throw new PALServiceException("Exception while retrieving cloudBlobContainerReference", e);
        }
        return container;
    }

    /**
     *  fetchDownloadTemplate
     *
     * @param downloadTemplateName string
     * return CloudBlobBlob
     */
    @Override
    @Cacheable(value = "palDownloadTemplateEhCache", key = "#downloadTemplateName")
    public CloudBlockBlob fetchDownloadTemplate(String downloadTemplateName) {
        CloudBlobContainer container = getCloudBlobContainer(palContainer);
        CloudBlobDirectory mainDirectory = null;
        try {
            mainDirectory = container.getDirectoryReference(downloadTemplatesFolder);
            return mainDirectory.getBlockBlobReference(downloadTemplateName);
        } catch (URISyntaxException | StorageException e) {
            throw new PALServiceException("Exception while retrieving blob", e.toString());
        }
    }

    /**
     * readFromBlob
     *
     * @param uriString
     * @return CloudBlockBlob
     * @throws StorageException
     * @throws PALServiceException
     * @throws URISyntaxException
     */
    public CloudBlockBlob readFromBlob(String uriString) throws PALServiceException {
        log.debug("Entering AzureStorageDaoImpl.readFromBlob method");
        try {
            StorageCredentials storageCredentials = getStorageCredentials();
            URI uri = new URI(uriString);
            return new CloudBlockBlob(uri, storageCredentials);
        } catch (StorageException | URISyntaxException e) {
            throw new PALServiceException("Exception while retrieving blob", e.toString());
        }
    }

    @Retryable(value = {PALServiceException.class},
            maxAttemptsExpression = "#{${spring.max.connection.attempts}}",
            backoff = @Backoff(delayExpression = "#{${spring.backoff.connection.delay}}",
                    maxDelayExpression = "#{${spring.backoff.connection.max.delay}}",
                    multiplierExpression = "#{${spring.backoff.connection.multiplier}}"))
    public String writeObjectToBlob(String message, String fileName, String containerName, String mainDirectoryName) throws PALServiceException{
        log.debug("Entering writeObjectToBlob method:");
        String uri = null;
        CloudBlobContainer container;
        try {
            container = getCloudBlobContainer(containerName);
            CloudBlobDirectory mainDirectory = container.getDirectoryReference(mainDirectoryName);
            CloudBlockBlob blob = mainDirectory.getBlockBlobReference(fileName);
            byte[] messageInBytes = message.getBytes();
            blob.uploadFromByteArray(messageInBytes, 0, messageInBytes.length);
            log.info("ProductAttributeListingServiceImpl > Object loaded to blob : {}", uri);
        } catch (URISyntaxException | StorageException | IOException e) {
            throw new PALServiceException("Exception while writing to blob storage", e);
        }
        return uri;
    }

    @Override
    public String writeNotificationsBlob(Map<String, Object> message) throws PALServiceException{
        String fileName = (String) message.get(ApplicationConstant.Notification.EVENT_ID) + ApplicationConstant.TEXT_FILE_EXTENSION;
        return writeObjectToBlob(message.toString(), fileName, palContainer, notificationFailureFolder);
    }

    @Override
    public File downloadBlobToFile(String blobFileName, File tempFile) {
        CloudBlockBlob blob = fetchDownloadTemplate(blobFileName);
        try {
            blob.downloadToFile(tempFile.getAbsolutePath());
        } catch (PALServiceException | StorageException | IOException e) {
            throw new PALServiceException("Exception while reading from blob storage", e);
        }
        return tempFile;
    }


}
