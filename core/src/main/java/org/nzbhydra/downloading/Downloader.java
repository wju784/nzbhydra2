package org.nzbhydra.downloading;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.nzbhydra.GenericResponse;
import org.nzbhydra.config.DownloaderConfig;
import org.nzbhydra.config.NzbAddingType;
import org.nzbhydra.downloading.exceptions.DownloaderException;
import org.nzbhydra.searching.SearchResultEntity;
import org.nzbhydra.searching.SearchResultItem.DownloadType;
import org.nzbhydra.searching.SearchResultRepository;
import org.nzbhydra.searching.SearchResultWebTO;
import org.nzbhydra.searching.searchrequests.SearchRequest.SearchSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public abstract class Downloader {

    private static final Logger logger = LoggerFactory.getLogger(Downloader.class);

    @Autowired
    private NzbHandler nzbHandler;
    @Autowired
    private SearchResultRepository searchResultRepository;

    protected DownloaderConfig downloaderConfig;

    public void intialize(DownloaderConfig downloaderConfig) {
        this.downloaderConfig = downloaderConfig;
    }

    @Transactional
    public AddNzbsResponse addBySearchResultIds(List<SearchResultWebTO> searchResults, String category) {
        NzbAddingType addingType = downloaderConfig.getNzbAddingType();
        List<Long> addedNzbs = new ArrayList<>();
        try {
            for (SearchResultWebTO entry : searchResults) {
                Long guid = Long.valueOf(entry.getSearchResultId());
                String categoryToSend = Strings.isNullOrEmpty(category) ? entry.getOriginalCategory() : category;
                if (addingType == NzbAddingType.UPLOAD) {
                    NzbDownloadResult result = nzbHandler.getNzbByGuid(guid, downloaderConfig.getNzbAccessType(), SearchSource.INTERNAL);
                    String externalId = addNzb(result.getNzbContent(), result.getTitle(), categoryToSend);
                    result.getDownloadEntity().setExternalId(externalId);
                    nzbHandler.updateStatusByEntity(result.getDownloadEntity(), NzbDownloadStatus.NZB_ADDED);
                } else {
                    SearchResultEntity searchResultEntity = searchResultRepository.getOne(guid);
                    addLink(nzbHandler.getNzbDownloadLink(guid, false, DownloadType.NZB), searchResultEntity.getTitle(), categoryToSend);
                    //LATER: Use the external ID some way, perhaps store it or something
                    //At this point we don't have a DownloadEntity for which we could set the external status. When a link is added to the download it will download the NZB from us and only then
                    //will there be an entity. So just adding an link will not be considered a download. The external ID will have to be set using the title (for now)
                }
                addedNzbs.add(guid);
            }

        } catch (InvalidSearchResultIdException | DownloaderException e) {
            String message;
            if (e instanceof DownloaderException) {
                message = "Error while adding NZB(s) to downloader: " + e.getMessage();
            } else {
                message = e.getMessage();
            }
            logger.error(message);
            if (!addedNzbs.isEmpty()) {
                message += ".\n" + addedNzbs.size() + " were added successfully";
            }
            Set<Long> searchResultIds = Sets.newHashSet(searchResults.stream().map(x -> Long.valueOf(x.getSearchResultId())).collect(Collectors.toSet()));
            searchResultIds.removeAll(addedNzbs);
            return new AddNzbsResponse(false, message, addedNzbs, searchResultIds);
        }
        return new AddNzbsResponse(true, null, addedNzbs, Collections.emptyList());
    }


    public abstract GenericResponse checkConnection();

    public abstract List<String> getCategories();

    /**
     * @param link     Link to the NZB
     * @param title    Title to tell the downloader
     * @param category Category to file under
     * @return ID returned by the downloader
     * @throws DownloaderException
     */
    public abstract String addLink(String link, String title, String category) throws DownloaderException;

    /**
     * @param content  NZB content to upload
     * @param title    Title to tell the downloader
     * @param category Category to file under
     * @return ID returned by the downloader
     * @throws DownloaderException
     */
    public abstract String addNzb(String content, String title, String category) throws DownloaderException;

    @Data
    @AllArgsConstructor
    public class AddNzbsResponse {
        private boolean successful;
        private String message;
        private Collection<Long> addedIds;
        private Collection<Long> missedIds;
    }

}
