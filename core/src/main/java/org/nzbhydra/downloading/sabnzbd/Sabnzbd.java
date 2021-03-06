package org.nzbhydra.downloading.sabnzbd;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.nzbhydra.GenericResponse;
import org.nzbhydra.downloading.Downloader;
import org.nzbhydra.downloading.exceptions.DownloaderException;
import org.nzbhydra.downloading.exceptions.DownloaderUnreachableException;
import org.nzbhydra.okhttp.HydraOkHttp3ClientHttpRequestFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.List;

@Component
public class Sabnzbd extends Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Sabnzbd.class);
    private ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private HydraOkHttp3ClientHttpRequestFactory requestFactory;

    private UriComponentsBuilder getBaseUrl() {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(downloaderConfig.getUrl()).pathSegment("api");
        if (!Strings.isNullOrEmpty(downloaderConfig.getApiKey())) {
            builder.queryParam("apikey", downloaderConfig.getApiKey());
        }
        builder.queryParam("output", "json");
        return builder;
    }

    @Override
    public String addLink(String url, String title, String category) throws DownloaderException {
        logger.debug("Sending link for NZB {} to sabnzbd", title);
        if (!title.toLowerCase().endsWith(".nzbd")) {
            title += ".nzb";
        }
        UriComponentsBuilder urlBuilder = getBaseUrl();
        urlBuilder.queryParam("mode", "addurl").queryParam("name", url).queryParam("nzbname", title).queryParam("priority", "-100");
        if (!Strings.isNullOrEmpty(category)) {
            urlBuilder.queryParam("cat", category);
        }
        String nzoId = sendAddNzbLinkCommand(urlBuilder, null, HttpMethod.POST);
        logger.info("Successfully added link {} for NZB \"{}\" to sabnzbd queue with ID {}", url, title, nzoId);
        return nzoId;
    }

    private String sendAddNzbLinkCommand(UriComponentsBuilder urlBuilder, HttpEntity httpEntity, HttpMethod httpMethod) throws DownloaderException {
        try {
            ResponseEntity<AddNzbResponse> response = restTemplate.exchange(urlBuilder.build().encode().toUri(), httpMethod, httpEntity, AddNzbResponse.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new DownloaderException("Downloader returned status code " + response.getStatusCode());
            }
            if (!response.getBody().isStatus()) {
                throw new DownloaderException("Downloader says NZB was not added successfully.");
            }
            if (response.getBody().getNzoIds().isEmpty()) {
                throw new DownloaderException("Sabnzbd says NZB was added successfully but didn't return an NZO ID");
            }
            return response.getBody().getNzoIds().get(0);
        } catch (RestClientException e) {
            throw new DownloaderUnreachableException("Error while adding NZB(s)", e);
        }
    }

    @Override
    public String addNzb(String fileContent, String title, String category) throws DownloaderException {
        //Using OKHTTP here because RestTemplate wouldn't work
        logger.debug("Uploading NZB {} to sabnzbd", title);
        UriComponentsBuilder urlBuilder = getBaseUrl();
        urlBuilder.queryParam("mode", "addfile").queryParam("nzbname", title).queryParam("priority", "-100");
        if (!Strings.isNullOrEmpty(category)) {
            urlBuilder.queryParam("cat", category);
        }

        RequestBody formBody = new MultipartBody.Builder().addFormDataPart("name", title, RequestBody.create(MediaType.parse(org.springframework.http.MediaType.APPLICATION_XML_VALUE), fileContent)).build();
        Request request = new Request.Builder()
                .url(urlBuilder.toUriString())
                .post(formBody)
                .build();
        OkHttpClient client = requestFactory.getOkHttpClientBuilder(urlBuilder.build().encode().toUri()).build();
        try (Response response = client.newCall(request).execute(); ResponseBody body = response.body()) {
            if (!response.isSuccessful()) {
                throw new DownloaderException("Downloader returned status code " + response.code());
            }
            AddNzbResponse addNzbResponse = objectMapper.readValue(body.string(), AddNzbResponse.class);
            if (addNzbResponse.getNzoIds().isEmpty()) {
                throw new DownloaderException("Sabnzbd says NZB was added successfully but didn't return an NZO ID");
            }
            if (addNzbResponse.getNzoIds().isEmpty()) {
                throw new DownloaderException("Sabnzbd says NZB was added successfully but didn't return an NZO ID");
            }
            String nzoId = addNzbResponse.getNzoIds().get(0);
            logger.info("Successfully added NZB \"{}\" to sabnzbd queue with ID {}", title, nzoId);
            return nzoId;
        } catch (IOException e) {
            throw new DownloaderException("Error while communicating with downloader: " + e.getMessage());
        }

    }


    @Override
    public GenericResponse checkConnection() {
        logger.debug("Checking connection");
        UriComponentsBuilder baseUrl = getBaseUrl();
        try {
            restTemplate.exchange(baseUrl.queryParam("mode", "get_cats").toUriString(), HttpMethod.GET, null, CategoriesResponse.class);
            logger.info("Connection check with sabnzbd using URL {} successful", baseUrl.toUriString());
            return new GenericResponse(true, null);
        } catch (RestClientException e) {
            logger.info("Connection check with sabnzbd using URL {} failed: {}", baseUrl.toUriString(), e.getMessage());
            return new GenericResponse(false, e.getMessage());
        }
    }

    @Override
    public List<String> getCategories() {
        logger.debug("Loading list of categories");
        UriComponentsBuilder uriBuilder = getBaseUrl().queryParam("mode", "get_cats");
        return restTemplate.getForObject(uriBuilder.build().toUri(), CategoriesResponse.class).getCategories();
    }

}
