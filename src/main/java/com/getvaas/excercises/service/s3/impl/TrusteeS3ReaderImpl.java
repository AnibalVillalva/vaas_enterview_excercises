package com.getvaas.excercises.service.s3.impl;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.getvaas.excercises.service.s3.TrusteeS3Reader;

import java.util.List;
import java.util.stream.Collectors;

public class TrusteeS3ReaderImpl implements TrusteeS3Reader {

    @Override
    public List<String> retrieveCreditFileNames() {

        String bucket_name = "fefobucket";

        System.out.format("Objects in S3 bucket %s:\n", bucket_name);
        final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1).build();

        ListObjectsV2Result result = s3.listObjectsV2(bucket_name);
        List<S3ObjectSummary> objects = result.getObjectSummaries();

        for (S3ObjectSummary os : objects) {
            System.out.println("* " + os.getKey());
        }

        List<String> pdfNames = objects.stream()
                .map(s3ObjectSummary -> s3ObjectSummary.getKey())
                .collect(Collectors.toList());

        return pdfNames;

    }
}
