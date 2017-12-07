/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 * 
 * Copy from mule 3.4
 * 
 */
package orufeo.iasion.http;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HeaderElement;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.cookie.MalformedCookieException;
import org.apache.log4j.Logger;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.transport.MessageTypeNotSupportedException;
import org.mule.transport.http.CookieHelper;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;
import org.mule.transport.http.HttpRequest;
import org.mule.util.CaseInsensitiveHashMap;
import org.mule.util.PropertiesUtils;

/**
 * @author ananas
 * Override Mule's HttpFactoryfor case insensitive comparaison of Https Headers (Cookie vs cookie) for HTTP2 protocol
 */
public class HttpMuleMessageFactory extends org.mule.transport.http.HttpMuleMessageFactory 
{
	private static Logger log = Logger.getLogger(HttpMultipartMuleMessageFactory.class);
    private static final String DEFAULT_ENCODING = "UTF-8";

    private boolean enableCookies = false;
    private String cookieSpec;
    
    /**
     * 
     * @param context
     */
    public HttpMuleMessageFactory(MuleContext context)
    {
        super(context);
    }

    @Override
    protected void addProperties(DefaultMuleMessage message, Object transportMessage) throws Exception
    {
        String method;
        HttpVersion httpVersion;
        String uri;
        String statusCode = null;
        Map<String, Object> headers;  
        Map<String, Object> httpHeaders = new HashMap<String, Object>();
        Map<String, Object> queryParameters = new HashMap<String, Object>();

        if (transportMessage instanceof HttpRequest)
        {
            HttpRequest httpRequest = (HttpRequest) transportMessage;
            method = httpRequest.getRequestLine().getMethod();
            httpVersion = httpRequest.getRequestLine().getHttpVersion();
            uri = httpRequest.getRequestLine().getUri();
            headers = convertHeadersToMap(httpRequest.getHeaders(), uri);
            convertMultiPartHeaders(headers);
        }
        else if (transportMessage instanceof HttpMethod)
        {
            HttpMethod httpMethod = (HttpMethod) transportMessage;
            method = httpMethod.getName();
            httpVersion = HttpVersion.parse(httpMethod.getStatusLine().getHttpVersion());
            uri = httpMethod.getURI().toString();
            statusCode = String.valueOf(httpMethod.getStatusCode());
            headers = convertHeadersToMap(httpMethod.getResponseHeaders(), uri);
        }
        else
        {
            // This should never happen because of the supported type checking in our superclass
            throw new MessageTypeNotSupportedException(transportMessage, getClass());
        }

        rewriteConnectionAndKeepAliveHeaders(headers);

        headers = processIncomingHeaders(headers);

        httpHeaders.put(HttpConnector.HTTP_HEADERS, new HashMap<String, Object>(headers));

        String encoding = getEncoding(headers);
        
        queryParameters.put(HttpConnector.HTTP_QUERY_PARAMS, processQueryParams(uri, encoding));

        //Make any URI params available ans inbound message headers
        addUriParamsAsHeaders(headers, uri);

        headers.put(HttpConnector.HTTP_METHOD_PROPERTY, method);
        headers.put(HttpConnector.HTTP_REQUEST_PROPERTY, uri);
        headers.put(HttpConnector.HTTP_VERSION_PROPERTY, httpVersion.toString());
        if (enableCookies)
        {
            headers.put(HttpConnector.HTTP_COOKIE_SPEC_PROPERTY, cookieSpec);
        }

        if (statusCode != null)
        {
            headers.put(HttpConnector.HTTP_STATUS_PROPERTY, statusCode);
        }

        message.addInboundProperties(headers);
        message.addInboundProperties(httpHeaders);
        message.addInboundProperties(queryParameters);

        // The encoding is stored as message property. To avoid overriding it from the message
        // properties, it must be initialized last
        initEncoding(message, encoding);
    }

    protected Map<String, Object> processIncomingHeaders(Map<String, Object> headers) throws Exception
    {
        Map<String, Object> outHeaders = new CaseInsensitiveHashMap();

        for (Map.Entry<String, Object> header : headers.entrySet())
        {
            String headerName = header.getKey();

            // fix Mule headers?
            if (headerName.startsWith("X-MULE"))
            {
                headerName = headerName.substring(2);
            }

            // accept header & value
            outHeaders.put(headerName, header.getValue());
        }

        return outHeaders;
    }

    Map<String, Object> convertHeadersToMap(final Header[] headersArray, final String uri)
        throws URISyntaxException
    {
        Map<String, Object> headersMap = new CaseInsensitiveHashMap();
        for (int i = 0; i < headersArray.length; i++)
        {
            final Header header = headersArray[i];
            // Cookies are a special case because there may be more than one
            // cookie.
            String headerName = header.getName().toLowerCase();
            
			if (HttpConnector.HTTP_COOKIES_PROPERTY.equalsIgnoreCase(header.getName()) // modification en equalsIgnoreCase pour conformité RFC HTTP
                || HttpConstants.HEADER_COOKIE.equalsIgnoreCase(header.getName())) // modification en equalsIgnoreCase pour conformité RFC HTTP
            {
                putCookieHeaderInMapAsAServer(headersMap, header, uri);
            }
            else if (HttpConstants.HEADER_COOKIE_SET.equalsIgnoreCase(header.getName())) // modification en equalsIgnoreCase pour conformité RFC HTTP
            {
                putCookieHeaderInMapAsAClient(headersMap, header, uri);
            }
            else
            {
                if (headersMap.containsKey(header.getName()))
                {
                    if (headersMap.get(header.getName()) instanceof String)
                    {
                        // concat
                        headersMap.put(headerName,
                            headersMap.get(header.getName()) + "," + header.getValue());
                    }
                    else
                    {
                        // override
                        headersMap.put(headerName, header.getValue());
                    }
                }
                else
                {
                    headersMap.put(headerName, header.getValue());
                }
            }
        }
        return headersMap;
    }

    private void putCookieHeaderInMapAsAClient(Map<String, Object> headersMap, final Header header, String uri)
        throws URISyntaxException
    {
        try
        {
            final Cookie[] newCookies = CookieHelper.parseCookiesAsAClient(header.getValue(), cookieSpec,
                new URI(uri));
            final Object preExistentCookies = headersMap.get(HttpConstants.HEADER_COOKIE_SET);
            final Object mergedCookie = CookieHelper.putAndMergeCookie(preExistentCookies, newCookies);
            headersMap.put(HttpConstants.HEADER_COOKIE_SET, mergedCookie);
        }
        catch (MalformedCookieException e)
        {
            log.warn("Received an invalid cookie: " + header, e);
        }
    }

    private void putCookieHeaderInMapAsAServer(Map<String, Object> headersMap, final Header header, String uri)
        throws URISyntaxException
    {
        if (enableCookies)
        {
            Cookie[] newCookies = CookieHelper.parseCookiesAsAServer(header.getValue(), new URI(uri));
            if (newCookies.length > 0)
            {
                Object oldCookies = headersMap.get(HttpConnector.HTTP_COOKIES_PROPERTY);
                Object mergedCookies = CookieHelper.putAndMergeCookie(oldCookies, newCookies);
                headersMap.put(HttpConnector.HTTP_COOKIES_PROPERTY, mergedCookies);
            }
        }
    }

    private String getEncoding(Map<String, Object> headers)
    {
        String encoding = DEFAULT_ENCODING;
        Object contentType = headers.get(HttpConstants.HEADER_CONTENT_TYPE);
        if (contentType != null)
        {
            // use HttpClient classes to parse the charset part from the Content-Type
            // header (e.g. "text/html; charset=UTF-16BE")
            Header contentTypeHeader = new Header(HttpConstants.HEADER_CONTENT_TYPE,
                                                  contentType.toString());
            HeaderElement values[] = contentTypeHeader.getElements();
            if (values.length == 1)
            {
                NameValuePair param = values[0].getParameterByName("charset");
                if (param != null)
                {
                    encoding = param.getValue();
                }
            }
        }
        return encoding;
    }
    
    private void initEncoding(MuleMessage message, String encoding)
    {
        message.setEncoding(encoding);
    }

    private void rewriteConnectionAndKeepAliveHeaders(Map<String, Object> headers)
    {
        // rewrite Connection and Keep-Alive headers based on HTTP version
        String headerValue;
        if (!isHttp11(headers))
        {
            String connection = (String) headers.get(HttpConstants.HEADER_CONNECTION);
            if ((connection != null) && connection.equalsIgnoreCase("close"))
            {
                headerValue = Boolean.FALSE.toString();
            }
            else
            {
                headerValue = Boolean.TRUE.toString();
            }
        }
        else
        {
            headerValue = (headers.get(HttpConstants.HEADER_CONNECTION) != null
                    ? Boolean.TRUE.toString()
                    : Boolean.FALSE.toString());
        }

        headers.put(HttpConstants.HEADER_CONNECTION, headerValue);
        headers.put(HttpConstants.HEADER_KEEP_ALIVE, headerValue);
    }

    /**
     * 
     * @param headers
     * @return
     */
    private boolean isHttp11(Map<String, Object> headers)
    {
        String httpVersion = (String) headers.get(HttpConnector.HTTP_VERSION_PROPERTY);
        return !HttpConstants.HTTP10.equalsIgnoreCase(httpVersion);
    }

    /**
     * 
     */
    protected void addUriParamsAsHeaders(Map headers, String uri)
    {
        int i = uri.indexOf("?");
        String queryString = "";
        if(i > -1)
        {
            queryString = uri.substring(i + 1);
            headers.putAll(PropertiesUtils.getPropertiesFromQueryString(queryString));
        }
        headers.put(HttpConnector.HTTP_QUERY_STRING, queryString);
    }
    
    /**
     * 
     */
    protected Map<String, Object> processQueryParams(String uri, String encoding) throws UnsupportedEncodingException
    {
        Map<String, Object> httpParams = new HashMap<String, Object>();
        
        int i = uri.indexOf("?");
        if(i > -1)
        {
            String queryString = uri.substring(i + 1);
            for (StringTokenizer st = new StringTokenizer(queryString, "&"); st.hasMoreTokens();)
            {
                String token = st.nextToken();
                int idx = token.indexOf('=');
                if (idx < 0)
                {
                    addQueryParamToMap(httpParams, unescape(token, encoding), null);
                }
                else if (idx > 0)
                {
                    addQueryParamToMap(httpParams, unescape(token.substring(0, idx), encoding),
                        unescape(token.substring(idx + 1), encoding));
                }
            }
        }
            
        return httpParams;
    }

    /**
     * 
     * @param httpParams
     * @param key
     * @param value
     */
    private void addQueryParamToMap(Map<String, Object> httpParams, String key, String value)
    {
        Object existingValue = httpParams.get(key);
        if (existingValue == null)
        {
            httpParams.put(key, value);
        }
        else if (existingValue instanceof List)
        {
            @SuppressWarnings("unchecked")
			List<String> list = (List<String>) existingValue;
            list.add(value);
        }
        else if (existingValue instanceof String)
        {
            List<String> list = new ArrayList<String>();
            list.add((String) existingValue);
            list.add(value);
            httpParams.put(key, list);
        }
    }
    
    private String unescape(String escapedValue, String encoding) throws UnsupportedEncodingException
    {
        if(escapedValue != null)
        {
            return URLDecoder.decode(escapedValue, encoding);
        }
        return escapedValue;
    }

    public void setEnableCookies(boolean enableCookies)
    {
        this.enableCookies = enableCookies;
        super.setEnableCookies(enableCookies);
    }

    public void setCookieSpec(String cookieSpec)
    {
        this.cookieSpec = cookieSpec;
        super.setCookieSpec(cookieSpec);
    }
    
}
