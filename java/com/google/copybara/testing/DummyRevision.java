/*
 * Copyright (C) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.copybara.testing;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.copybara.Change;
import com.google.copybara.LabelFinder;
import com.google.copybara.Origin;
import com.google.copybara.RepoException;
import com.google.copybara.Revision;
import com.google.copybara.authoring.Author;
import com.google.copybara.authoring.Authoring;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import javax.annotation.Nullable;

/**
 * A reference of a change used for testing. This can be used with a {@link DummyOrigin} instance or
 * without an actual {@link Origin} implementation.
 */
public class DummyRevision implements Revision {

  private static final Author DEFAULT_AUTHOR = new Author("Dummy Author", "no-reply@dummy.com");

  private final String reference;
  private final String message;
  private final Author author;
  final Path changesBase;
  private final Instant timestamp;
  @Nullable
  private final String contextReference;
  private final ImmutableMap<String, String> referenceLabels;
  private final boolean matchesGlob;
  private final ImmutableMap<String, String> descriptionLabels;

  public DummyRevision(String reference) {
    this(reference, "DummyReference message", DEFAULT_AUTHOR,
        Paths.get("/DummyReference", reference), /*timestamp=*/null,
        /*contextReference=*/ null, /*referenceLabels=*/ ImmutableMap.of(),
         /*matchesGlob=*/true);
  }

  DummyRevision(
      String reference, String message, Author author, Path changesBase,
      @Nullable Instant timestamp, @Nullable String contextReference,
      ImmutableMap<String, String> referenceLabels, boolean matchesGlob) {
    this.reference = Preconditions.checkNotNull(reference);
    this.message = Preconditions.checkNotNull(message);
    this.author = Preconditions.checkNotNull(author);
    this.changesBase = Preconditions.checkNotNull(changesBase);
    this.timestamp = timestamp;
    this.contextReference = contextReference;
    this.referenceLabels = Preconditions.checkNotNull(referenceLabels);
    this.matchesGlob = matchesGlob;

    ImmutableMap.Builder<String, String> labels = ImmutableMap.builder();
    for (String line : message.split("\n")) {
      LabelFinder labelFinder = new LabelFinder(line);
      if (labelFinder.isLabel()) {
        labels.put(labelFinder.getName(), labelFinder.getValue());
      }
    }
    this.descriptionLabels = labels.build();
  }

  /**
   * Returns an instance equivalent to this one but with the timestamp set to the specified value.
   */
  public DummyRevision withTimestamp(Instant newTimestamp) {
    return new DummyRevision(
        this.reference, this.message, this.author, this.changesBase, newTimestamp,
        this.contextReference, this.referenceLabels, this.matchesGlob);
  }

  public DummyRevision withAuthor(Author newAuthor) {
    return new DummyRevision(
        this.reference, this.message, newAuthor, this.changesBase, this.timestamp,
        this.contextReference, this.referenceLabels, this.matchesGlob);
  }

  public DummyRevision withContextReference(String contextReference) {
    Preconditions.checkNotNull(contextReference);
    return new DummyRevision(
        this.reference, this.message, this.getAuthor(), this.changesBase, this.timestamp,
        contextReference, this.referenceLabels, this.matchesGlob);
  }

  @Nullable
  @Override
  public Instant readTimestamp() throws RepoException {
    return timestamp;
  }

  @Override
  public String asString() {
    return reference;
  }

  @Override
  public String getLabelName() {
    return DummyOrigin.LABEL_NAME;
  }

  Change<DummyRevision> toChange(Authoring authoring) {
    Author safeAuthor = authoring.useAuthor(this.author.getEmail())
        ? this.author
        : authoring.getDefaultAuthor();
    return new Change<>(this, safeAuthor, message,
        ZonedDateTime.ofInstant(timestamp, ZoneId.systemDefault()), descriptionLabels);
  }

  @Nullable
  @Override
  public String contextReference() {
    return contextReference;
  }

  @Override
  public ImmutableMap<String, String> associatedLabels() {
    return referenceLabels;
  }

  public Author getAuthor() {
    return author;
  }

  public boolean matchesGlob() {
    return matchesGlob;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("reference", reference)
        .add("message", message)
        .add("author", author)
        .add("changesBase", changesBase)
        .add("timestamp", timestamp)
        .add("descriptionLabels", descriptionLabels)
        .toString();
  }
}
