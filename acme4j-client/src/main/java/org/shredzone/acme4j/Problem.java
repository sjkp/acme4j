/*
 * acme4j - Java ACME client
 *
 * Copyright (C) 2017 Richard "Shred" Körber
 *   http://acme4j.shredzone.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package org.shredzone.acme4j;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import org.shredzone.acme4j.exception.AcmeProtocolException;
import org.shredzone.acme4j.toolbox.JSON;

/**
 * Represents a JSON Problem.
 *
 * @see <a href="https://tools.ietf.org/html/rfc7807">RFC 7807</a>
 */
public class Problem implements Serializable {
    private static final long serialVersionUID = -8418248862966754214L;

    private final URL baseUrl;
    private final JSON problemJson;

    /**
     * Creates a new {@link Problem} object.
     *
     * @param problem
     *            Problem as JSON structure
     * @param baseUrl
     *            Document's base {@link URL} to resolve relative URIs against
     */
    public Problem(JSON problem, URL baseUrl) {
        this.problemJson = problem;
        this.baseUrl = baseUrl;
    }

    /**
     * Returns the problem type. It is always an absolute URI.
     */
    public URI getType() {
        try {
            String type = problemJson.get("type").asString();
            return type != null ? baseUrl.toURI().resolve(type) : null;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Bad base URL", ex);
        }
    }

    /**
     * Returns a short, human-readable summary of the problem. The text may be localized
     * if supported by the server. {@code null} if the server did not provide a title.
     *
     * @see #toString()
     */
    public String getTitle() {
        return problemJson.get("title").asString();
    }

    /**
     * Returns a detailed and specific human-readable explanation of the problem. The
     * text may be localized if supported by the server.
     *
     * @see #toString()
     */
    public String getDetail() {
        return problemJson.get("detail").asString();
    }

    /**
     * Returns an URI that identifies the specific occurence of the problem. It is always
     * an absolute URI.
     */
    public URI getInstance() {
        try {
            String instance = problemJson.get("instance").asString();
            return instance != null ? baseUrl.toURI().resolve(instance) : null;
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException("Bad base URL", ex);
        }
    }

    /**
     * Returns the domain this problem relates to. May be {@code null}.
     */
    public String getDomain() {
        JSON identifier = problemJson.get("identifier").asObject();
        if (identifier == null) {
            return null;
        }

        String type = identifier.get("type").asString();
        if (!"dns".equals(type)) {
            throw new AcmeProtocolException("Cannot process a " + type + " identifier");
        }

        return identifier.get("value").asString();
    }

    /**
     * Returns a list of sub-problems. May be empty, but is never {@code null}.
     */
    public List<Problem> getSubProblems() {
        return unmodifiableList(
                problemJson.get("subproblems")
                        .asArray().stream()
                        .map(o -> o.asProblem(baseUrl))
                        .collect(toList())
        );
    }

    /**
     * Returns the problem as {@link JSON} object, to access other fields.
     *
     * @return Problem as {@link JSON} object
     */
    public JSON asJSON() {
        return problemJson;
    }

    /**
     * Returns a human-readable description of the problem, that is as specific as
     * possible. The description may be localized if supported by the server.
     * <p>
     * If {@link #getSubProblems()} exist, they will be appended.
     * <p>
     * Technically, it returns {@link #getDetail()}. If not set, {@link #getTitle()} is
     * returned instead. As a last resort, {@link #getType()} is returned.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (getDetail() != null) {
            sb.append(getDetail());
        } else if (getTitle() != null) {
            sb.append(getTitle());
        } else {
            sb.append(getType());
        }

        List<Problem> subproblems = getSubProblems();

        if (!subproblems.isEmpty()) {
            sb.append(" (");
            boolean first = true;
            for (Problem sub : subproblems) {
                if (!first) {
                    sb.append(" ‒ ");
                }
                sb.append(sub.toString());
                first = false;
            }
            sb.append(')');
        }

        return sb.toString();
    }

}
