package org.nzbhydra.searching;

import com.google.common.base.Objects;
import lombok.Getter;
import org.hibernate.annotations.GenericGenerator;
import org.nzbhydra.indexers.IndexerEntity;
import org.nzbhydra.searching.SearchResultItem.DownloadType;

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
import javax.validation.constraints.NotNull;
import java.time.Instant;


@Entity
@Getter
@Table(name = "searchresult"
        , indexes = {
        @Index(columnList = "indexer_id,indexerguid", unique = true)}
)
public class SearchResultEntity {


    @GenericGenerator(
            name = "search-result-sequence",
            strategy = "org.nzbhydra.searching.SearchResultSequenceGenerator",
            parameters = @org.hibernate.annotations.Parameter(
                    name = "sequence_name",
                    value = "hibernate_sequence"
            )
    )
    @Id
    @GeneratedValue(generator = "search-result-sequence", strategy = GenerationType.SEQUENCE)
    protected long id;

    @ManyToOne
    @NotNull
    protected IndexerEntity indexer;

    @Convert(converter = com.github.marschall.threeten.jpa.InstantConverter.class)
    protected Instant firstFound;

    @NotNull
    @Column(length = 4000)
    protected String title;

    @Column(name = "indexerguid")
    @NotNull
    protected String indexerGuid;
    @Column(length = 4000)
    protected String link;
    @Column(length = 4000)
    protected String details;
    @Enumerated(EnumType.STRING)
    protected DownloadType downloadType;
    @Convert(converter = com.github.marschall.threeten.jpa.InstantConverter.class)
    protected Instant pubDate;

    public SearchResultEntity() {
    }

    public SearchResultEntity(IndexerEntity indexer, Instant firstFound, String title, String indexerGuid, String link, String details, DownloadType downloadType, Instant pubDate) {
        this.indexer = indexer;
        this.firstFound = firstFound;
        this.title = title;
        this.indexerGuid = indexerGuid;
        this.link = link;
        this.details = details;
        this.downloadType = downloadType;
        this.pubDate = pubDate;
    }

    public void setId(long id) {
        this.id = id;
    }

    public void setIndexer(IndexerEntity indexer) {
        this.indexer = indexer;
    }

    public void setFirstFound(Instant firstFound) {
        this.firstFound = firstFound;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setIndexerGuid(String indexerGuid) {
        this.indexerGuid = indexerGuid;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setDownloadType(DownloadType downloadType) {
        this.downloadType = downloadType;
    }

    public void setPubDate(Instant pubDate) {
        this.pubDate = pubDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SearchResultEntity that = (SearchResultEntity) o;
        if (this.id != 0 || that.id != 0) {
            return this.id == that.id;
        }
        return Objects.equal(indexer, that.indexer) &&
                Objects.equal(indexerGuid, that.indexerGuid);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
