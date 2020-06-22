package org.hjson;

/**
 * This class represents the various comment styles that can be used when adding
 * new comments to a {@link JsonValue}.
 */
public enum CommentStyle {
    /**
     * Hash style comments, indicated by placing a <code>#</code> symbol at the
     * start of the comment, followed by the beginning of each new line.
     */
    HASH,
    /**
     * The C style line comment, indicated by placing a <code>//</code> at the
     * start of the comment, followed by the beginning of each new line.
     */
    LINE,
    /**
     * A block style comment, indicated by placing a <code>/*</code> at the
     * start of the comment, followed by a <code>* /</code> (with no spaces) at
     * the very end of the comment.
     */
    BLOCK
}
