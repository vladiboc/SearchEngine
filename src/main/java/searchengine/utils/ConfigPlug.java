package searchengine.utils;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import searchengine.config.ConnectionHeaders;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.config.SpringSettings;

import java.net.URL;

@Component
public class ConfigPlug {
    private static SpringSettings springSettings;
    private static SitesList sites;
    private static ConnectionHeaders headersFromConfig;

    private ConfigPlug(
            SpringSettings springSettings, SitesList sites, ConnectionHeaders connectionHeaders
    ) {
        ConfigPlug.springSettings = springSettings;
        ConfigPlug.sites = sites;
        ConfigPlug.headersFromConfig = connectionHeaders;
    }

    public static SpringSettings getSpringSettings() {
        return ConfigPlug.springSettings;
    }

    public static SitesList getSitesList() {
        return ConfigPlug.sites;
    }

    public static ConnectionHeaders getHeadersFromConfig() {
        return ConfigPlug.headersFromConfig;
    }

    public static @Nullable Site searchInConfiguration(URL requestedUrl) {
        for (Site site : ConfigPlug.sites.getSites()) {
            URL siteFromConfigUrl = WebUtils.makeUrlFromString(site.getUrl());
            if (siteFromConfigUrl == null) {
                continue;
            }
            if (requestedUrl.getHost().equals(siteFromConfigUrl.getHost())) {
                return site;
            }
        }
        return null;
    }

}