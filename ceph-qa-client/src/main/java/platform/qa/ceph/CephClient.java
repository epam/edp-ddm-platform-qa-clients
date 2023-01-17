/*
 * Copyright 2022 EPAM Systems.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package platform.qa.ceph;

import lombok.SneakyThrows;
import platform.qa.entities.Ceph;

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.google.common.collect.Lists;

/**
 * Client to work with Ceph storage.
 * Use Ceph POJO as constructor parameter.
 */
public class CephClient {
    private final AmazonS3 client;

    public CephClient(Ceph ceph) {
        var credentials = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(ceph.getAccessKey(), ceph.getSecretKey()));

        var clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        client = AmazonS3ClientBuilder.standard()
                .withCredentials(credentials)
                .withClientConfiguration(clientConfig)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(ceph.getHost(), null))
                .withPathStyleAccessEnabled(true)
                .build();
    }

    public Bucket createBucket(String bucketName) {
        return client.createBucket(bucketName);
    }

    public List<Bucket> getBuckets() {
        return client.listBuckets();
    }

    public List<String> getListOfFilesFromBucket(String bucketName) {
        ObjectListing listing = client.listObjects(bucketName);
        return listing != null ?
                listing.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList()) :
                Lists.newLinkedList();
    }

    public boolean isBucketExists(String bucketName) {
        List<Bucket> bucketList = client.listBuckets();
        return bucketList != null && bucketList.stream().anyMatch(bucket -> bucket.getName().equals(bucketName));
    }

    public boolean isFileExistsInBucket(String bucketName, String cephKey) {
        List<String> filesInBucket = getListOfFilesFromBucket(bucketName);
        return !filesInBucket.isEmpty() && filesInBucket.stream().anyMatch(file -> file.equals(cephKey));
    }

    public void saveFileInBucket(String bucketName, String cephKey, InputStream stream, ObjectMetadata metadata) {
        client.putObject(bucketName, cephKey, stream, metadata);
    }

    public void saveFileInBucket(String bucketName, String cephKey, InputStream stream) {
        client.putObject(bucketName, cephKey, stream, new ObjectMetadata());
    }

    public void saveFileInBucket(String bucketName, String cephKey, File fileToSave) {
        client.putObject(bucketName, cephKey, fileToSave);
    }

    public ObjectMetadata getObjectMetadata(String bucketName, String cephKey) {
        S3Object object = uploadObjectFromBucket(bucketName, cephKey);
        return object != null ? object.getObjectMetadata() : new ObjectMetadata();
    }

    @SneakyThrows
    public File getFileFromBucket(String bucketName, String cephKey) {
        S3Object object = uploadObjectFromBucket(bucketName, cephKey);
        File file = new File("file.txt");

        try {
            FileUtils.copyInputStreamToFile(object.getObjectContent(), file);
            return file;
        } finally {
            file.deleteOnExit();
        }
    }

    @SneakyThrows
    public File getFileOfSpecificTypeFromBucket(String bucketName, String cephKey, String type) {
        S3Object object = uploadObjectFromBucket(bucketName, cephKey);
        File file = new File("file." + type);

        try {
            FileUtils.copyInputStreamToFile(object.getObjectContent(), file);
            return file;
        } finally {
            file.deleteOnExit();
        }
    }

    public void deleteFileFromBucket(String bucketName, String cephKey) {
        client.deleteObject(bucketName, cephKey);
    }

    private S3Object uploadObjectFromBucket(String bucketName, String cephKey) {
        return client.getObject(new GetObjectRequest(bucketName, cephKey));
    }
}
