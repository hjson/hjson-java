package org.hjson;

/**
 * This class represents the specific type of comment to be used in conjunction with a {@link JsonValue}.
 * Comments can be placed by calling {@link JsonValue#setComment(CommentType, CommentStyle, String)}
 * or another such variant.
 */
public enum CommentType {
    /**
     * Indicates that this comment precedes the value that it is paired with, to be placed one line
     * before the value. In the case of parent or root objects, this type indicates that the comment
     * is a header inside of the json file.
     */
    BOL,
    /**
     * Indicates that this comment follows the value that it is paired with, to be placed at the end
     * of the line. In the case of parent or root objects, this type indicates that the comment is a
     * footer inside of the json file.
     */
    EOL,
    /**
     * Indicates that this comment falls anywhere else in association with this value. This usually
     * implies that the value is inside of an empty object or array.
     */
    INTERIOR
}
