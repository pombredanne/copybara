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

import static com.google.common.truth.Truth.assertAbout;

import com.google.common.truth.FailureStrategy;
import com.google.common.truth.Subject;
import com.google.common.truth.SubjectFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

/**
 * Truth subjects for file assertions.
 */
public class FileSubjects {

  private static final SubjectFactory<PathSubject, Path> PATH_SUBJECT_FACTORY =
      new SubjectFactory<PathSubject, Path>() {
        @Override
        public PathSubject getSubject(FailureStrategy failureStrategy, Path target) {
          return new PathSubject(failureStrategy, target);
        }
      };

  private FileSubjects() {}

  /**
   * Truth subject that provides fluent methods for assertions on {@link Path}s.
   *
   * <p>For example:
   *
   *     assertThatPath(workdir)
   *       .containsFiles("file1", "file2")
   *       .containsFile("file3", "foo bar")
   *       .containsNoMoreFiles();
   */
  public static PathSubject assertThatPath(Path path) {
    return assertAbout(PATH_SUBJECT_FACTORY).that(path);
  }

  public static class PathSubject extends Subject<PathSubject, Path> {

    private final Set<Path> whitelistedPaths = new HashSet<>();

    PathSubject(FailureStrategy failureStrategy, Path target) {
      super(failureStrategy, target);
    }

    /**
     * Checks that the specific filenames exist relative to the path.
     */
    public PathSubject containsFiles(String... filenames) {
      for (String filename : filenames) {
        checkFile(filename);
      }
      return this;
    }

    /**
     * Checks that the specific filenames don't exist relative to the path.
     *
     * <p>Instead of this method, consider using {@link PathSubject#containsFile} combined with
     * {@link PathSubject#containsNoMoreFiles}.
     */
    public void containsNoFiles(String... filenames) {
      for (String filename : filenames) {
        Path filePath = getSubject().resolve(filename);
        if (Files.exists(filePath)) {
          fail("does not have file", filePath);
        }
      }
    }

    /**
     * Checks that a filename exists relative to the path, and that the contents match.
     */
    public PathSubject containsFile(String filename, String fileContents) throws IOException {
      Path filePath = checkFile(filename);
      String realContents = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
      if (!realContents.equals(fileContents)) {
        failWithCustomSubject(filename + " file content equals", fileContents, realContents);
      }
      return this;
    }

    /**
     * Checks that a filename exists relative to the path and that it is a symlink pointing to
     * target.
     */
    public PathSubject containsSymlink(String filename, String target) throws IOException {
      Path filePath =  getSubject().resolve(filename);
      Path targetPath = checkFile(target);

      if (!Files.isSymbolicLink(filePath)) {
        fail("%s is not a Symlink", filename);
      }
      Path realTarget = filePath.resolveSibling(Files.readSymbolicLink(filePath));
      if (!Files.isSameFile(realTarget, targetPath)) {
        failWithCustomSubject(filename + " Does not point to expected target", target, realTarget);
      }
      return this;
    }

    /**
     * Checks that there are no more files in the path.
     */
    public PathSubject containsNoMoreFiles() throws IOException {
      Files.walkFileTree(getSubject(), new SimpleFileVisitor<Path>() {
        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
          if (attrs.isRegularFile()) {
            Path relativeFile = getSubject().relativize(file);
            if (!whitelistedPaths.contains(relativeFile)) {
              fail("contains no more files", relativeFile.toString());
            }
          }
          return FileVisitResult.CONTINUE;
        }
      });
      return this;
    }

    private Path checkFile(String filename) {
      Path filePath = getSubject().resolve(filename);
      if (!Files.exists(filePath)) {
        fail("has file", filePath);
      }
      whitelistedPaths.add(getSubject().relativize(filePath));
      return filePath;
    }
  }
}
