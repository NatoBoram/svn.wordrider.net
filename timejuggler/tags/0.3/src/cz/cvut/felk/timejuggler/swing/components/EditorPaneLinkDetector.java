package cz.cvut.felk.timejuggler.swing.components;

import cz.cvut.felk.timejuggler.utilities.Browser;
import cz.cvut.felk.timejuggler.utilities.LogUtils;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Komponenta, ktera zvyraznuje emaily v textu. Pri drzeni CTRL+click otevira email v klientu.
 * @author Vity
 */
public class EditorPaneLinkDetector extends JEditorPane {
    private final static Logger logger = Logger.getLogger(EditorPaneLinkDetector.class.getName());
    private final static String EXAMPLE_EMAIL = "example@email.com";


    public EditorPaneLinkDetector() {
        super();
        final SyntaxDocument doc = new SyntaxDocument();
        this.setEditorKit(new StyledEditorKit() {
            public Document createDefaultDocument() {
                return doc;
            }
        });

        this.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                final JTextComponent source = (JTextComponent) e.getSource();
                if (source.isEditable())
                    return;
                final StyledDocument doc = (StyledDocument) source.getDocument();
                final Object attribute = doc.getCharacterElement(source.viewToModel(e.getPoint())).getAttributes().getAttribute("EMAIL");
                if (attribute != null) {
                    e.consume();
                    final String email = attribute.toString();
                    logger.info("EditorPaneLinkDetector opening email " + e);
                    Browser.openBrowser("mailto:" + email);
                }
            }

        });

        this.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    if (isEditable())
                        setEditable(false);
                } else {
                    if (!isEditable())
                        setEditable(true);
                }

            }

            public void keyReleased(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_CONTROL) {
                    setEditable(true);
                }

            }
        });
        insertExampleEmail(doc);

        this.addFocusListener(new FocusListener() {
            public void focusGained(FocusEvent e) {
                final JTextComponent source = (JTextComponent) e.getSource();
                if (EXAMPLE_EMAIL.equals(source.getText()))
                    source.setText("");
            }

            public void focusLost(FocusEvent e) {
                final JTextComponent source = (JTextComponent) e.getSource();
                if (source.getText().length() <= 0) {
                    insertExampleEmail((StyledDocument) source.getDocument());
                }
            }
        });
    }

    private void insertExampleEmail(StyledDocument doc) {
        SimpleAttributeSet example = new SimpleAttributeSet();
        StyleConstants.setForeground(example, Color.GRAY);
        try {
            doc.insertString(0, EXAMPLE_EMAIL, example);
        } catch (BadLocationException e) {
            LogUtils.processException(logger, e);
        }
    }

    public void setEmails(java.util.List<String> emails) {
        final Document document = this.getDocument();
        final StringBuilder builder = new StringBuilder();
        for (String email : emails) {
            builder.append(email).append('\n');
        }
        final String str = builder.toString();
        if (str.length() > 0)
            this.setText(""); //pro pripad ze je tam demo
        try {

            document.insertString(0, str, null);
        } catch (BadLocationException e) {
            LogUtils.processException(logger, e);
        }
    }

    public java.util.List<String> getEmails() {
        final String s = this.getText();
        final Pattern pattern = Pattern.compile("(^|s+|\\,|\\;)(([a-z0-9_\\-]+(\\.[_a-z0-9\\-]+)*@([_a-z0-9\\-]+\\.)+([a-z]{2}|aero|arpa|biz|com|coop|edu|gov|info|int|jobs|mil|museum|name|nato|net|org|pro|travel)))($|s+|\\,|\\;)", Pattern.CASE_INSENSITIVE);
        final Matcher matcher = pattern.matcher(s);
        final java.util.List<String> list = new ArrayList<String>();
        while (matcher.find()) {
            final String e = matcher.group(2);
            if (!EXAMPLE_EMAIL.equals(e))
                list.add(e);
        }
        return list;
    }


    static class SyntaxDocument extends DefaultStyledDocument {
        private DefaultStyledDocument doc;
        private Element rootElement;

        private MutableAttributeSet normal;
        private MutableAttributeSet keyword;
        private static final Pattern EMAIL_PATTERN = Pattern.compile("^([a-z0-9_\\-]+(\\.[_a-z0-9\\-]+)*@([_a-z0-9\\-]+\\.)+([a-z]{2}|aero|arpa|biz|com|coop|edu|gov|info|int|jobs|mil|museum|name|nato|net|org|pro|travel))$", Pattern.CASE_INSENSITIVE);
        private static final String DELIMITERS = " ,;:{}()[]+-/%<=>!&|^~*";


        public SyntaxDocument() {
            doc = this;
            rootElement = doc.getDefaultRootElement();
            putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");

            normal = new SimpleAttributeSet();
            StyleConstants.setForeground(normal, Color.RED);

            keyword = new SimpleAttributeSet();
            StyleConstants.setForeground(keyword, Color.BLUE);

        }

        /*
          *  Override to apply syntax highlighting after the document has been updated
          */
        public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
            super.insertString(offset, str, a);
            if (str.equals(EXAMPLE_EMAIL))
                return;
            processChangedLines(offset, str.length());
        }

        /*
          *  Override to apply syntax highlighting after the document has been updated
          */
        public void remove(int offset, int length) throws BadLocationException {
            super.remove(offset, length);
            processChangedLines(offset, 0);
        }

        /*
          *  Determine how many lines have been changed,
          *  then apply highlighting to each line
          */
        public void processChangedLines(int offset, int length)
                throws BadLocationException {
            String content = doc.getText(0, doc.getLength());

            //  The lines affected by the latest document update

            int startLine = rootElement.getElementIndex(offset);
            int endLine = rootElement.getElementIndex(offset + length);

            //  Do the actual highlighting

            for (int i = startLine; i <= endLine; i++) {
                applyHighlighting(content, i);
            }

        }

        /*
          *  Parse the line to determine the appropriate highlighting
          */
        private void applyHighlighting(String content, int line)
                throws BadLocationException {
            int startOffset = rootElement.getElement(line).getStartOffset();
            int endOffset = rootElement.getElement(line).getEndOffset() - 1;

            int lineLength = endOffset - startOffset;
            int contentLength = content.length();

            if (endOffset >= contentLength)
                endOffset = contentLength - 1;


            doc.setCharacterAttributes(startOffset, lineLength, normal, true);

            checkForTokens(content, startOffset, endOffset);
        }

        /*
        *	Parse the line for tokens to highlight
        */
        private void checkForTokens(String content, int startOffset, int endOffset) {
            while (startOffset <= endOffset) {
                //  skip the delimiters to find the start of a new token

                while (isDelimiter(content.substring(startOffset, startOffset + 1))) {
                    if (startOffset < endOffset)
                        startOffset++;
                    else
                        return;
                }

                startOffset = getOtherToken(content, startOffset, endOffset);
            }
        }

        /*
          *
          */
        private int getOtherToken(String content, int startOffset, int endOffset) {
            int endOfToken = startOffset + 1;

            while (endOfToken <= endOffset) {
                if (isDelimiter(content.substring(endOfToken, endOfToken + 1)))
                    break;

                endOfToken++;
            }

            String token = content.substring(startOffset, endOfToken);

            if (isKeyword(token)) {
                keyword.addAttribute("EMAIL", token);
                doc.setCharacterAttributes(startOffset, endOfToken - startOffset, keyword, false);
            }

            return endOfToken + 1;
        }

        /*
          *  Override for other languages
          */
        protected boolean isDelimiter(String character) {

            return Character.isWhitespace(character.charAt(0)) ||
                    DELIMITERS.indexOf(character) != -1;
        }


        /*
          *  Override for other languages
          */
        protected boolean isKeyword(String token) {
            //return keywords.contains(token);
//            System.out.println("token = " + token);
            final Matcher match = EMAIL_PATTERN.matcher(token);
            return (match.find());
        }
    }
}
