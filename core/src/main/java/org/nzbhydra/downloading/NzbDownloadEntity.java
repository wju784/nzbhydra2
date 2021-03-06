package org.nzbhydra.downloading;

import lombok.Data;
import org.nzbhydra.config.NzbAccessType;
import org.nzbhydra.searching.SearchResultEntity;
import org.nzbhydra.searching.searchrequests.SearchRequest.SearchSource;
import org.nzbhydra.web.SessionStorage;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import java.time.Instant;

@Data
@Entity
@Table(name = "indexernzbdownload", indexes = {@Index(name = "NZB_DOWNLOAD_EXT_ID", columnList = "EXTERNAL_ID")})
public class NzbDownloadEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    protected int id;
    @ManyToOne
    private SearchResultEntity searchResult;
    @Enumerated(EnumType.STRING)
    private NzbAccessType nzbAccessType;
    @Enumerated(EnumType.STRING)
    private SearchSource accessSource;
    @Convert(converter = com.github.marschall.threeten.jpa.InstantConverter.class)
    private Instant time = Instant.now();
    @Enumerated(EnumType.STRING)
    private NzbDownloadStatus status;
    private String error;
    private String username;
    private String ip;
    private String userAgent;
    /**
     * The age of the NZB at the time of downloading.
     */
    private Integer age;
    @Column(name = "EXTERNAL_ID")
    private String externalId;

    public NzbDownloadEntity(SearchResultEntity searchResult, NzbAccessType nzbAccessType, SearchSource accessSource, NzbDownloadStatus status, Integer age, String error) {
        this.searchResult = searchResult;
        this.nzbAccessType = nzbAccessType;
        this.accessSource = accessSource;
        this.status = status;
        this.time = Instant.now();
        this.username = SessionStorage.username.get();
        this.userAgent = SessionStorage.userAgent.get();
        this.ip = SessionStorage.IP.get();
        this.age = age;
        setError(error);
    }

    public void setError(String error) {
        if (error != null && error.length() > 4000) {
            this.error = error.substring(0,4000);
        } else {
            this.error = error;
        }
    }

    public NzbDownloadEntity() {
        this.time = Instant.now();
    }
}
