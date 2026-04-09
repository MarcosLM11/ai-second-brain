package brain.wiki;

import com.vladsch.flexmark.ext.wikilink.WikiLink;
import com.vladsch.flexmark.ext.wikilink.WikiLinkExtension;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.VisitHandler;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Extracts {@code [[wikilinks]]} from a markdown document using flexmark-java.
 *
 * <p>Links inside fenced code blocks and inline code spans are not extracted
 * because the flexmark AST does not create {@link WikiLink} nodes for verbatim content.
 * For aliased links ({@code [[page|alias]]}), the page target is returned, not the alias.
 */
public class WikilinkExtractor {

    private static final Parser PARSER;

    static {
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, List.of(WikiLinkExtension.create()));
        PARSER = Parser.builder(options).build();
    }

    /**
     * Returns the page targets of all wikilinks found in {@code content}.
     * Duplicates are preserved; order follows document order.
     */
    public static List<String> extract(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }

        var links = new ArrayList<String>();
        var document = PARSER.parse(content);

        NodeVisitor visitor = new NodeVisitor(
            new VisitHandler<>(WikiLink.class, link -> {
                // flexmark WikiLink convention: [[display|target]] — getLink() returns the part after |.
                // Obsidian format: [[page|alias]] — the page reference is the part BEFORE |.
                // When an alias is present, getText() holds the page reference (first part).
                // When there is no alias, getLink() holds the only part (the page reference).
                String pageRef = link.getText().length() > 0
                    ? link.getText().toString()
                    : link.getLink().toString();
                links.add(pageRef);
            })
        );
        visitor.visit(document);

        return List.copyOf(links);
    }
}
