/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cache.util;

import static com.teamdev.jxbrowser.engine.RenderingMode.OFF_SCREEN;

import com.teamdev.jxbrowser.browser.Browser;
import com.teamdev.jxbrowser.engine.Engine;
import com.teamdev.jxbrowser.logging.Level;
import com.teamdev.jxbrowser.logging.Logger;
import com.teamdev.jxbrowser.view.javafx.BrowserView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public final class JXBrowserTest1 extends Application {

    @Override
    public void start(Stage primaryStage) throws InterruptedException {
        Logger.level(Level.DEBUG);
        Engine engine = Engine.newInstance(OFF_SCREEN);
        Browser browser = engine.newBrowser();

        BrowserView view = BrowserView.newInstance(browser);

        BorderPane root = new BorderPane(view);
        Button button = new Button("Highlight");
        button.setOnAction(
                event -> selectAndHighlightItalicizeRange(browser, 1, 10, "#BE306A", "Tooltip"));
        root.setBottom(button);
        Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle("Hello World");
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(event -> engine.close());

        browser.navigation().loadUrlAndWait("/Users/mk2432/Desktop/eef1a1.html");
    }

    private void selectAndHighlightItalicizeRange(Browser browser, final int start, final int stop,
                                                  final String hexColor, final String tip) {
        if (hexColor != null && tip != null && !hexColor.trim().equals("") && !tip.trim()
                                                                                  .equals("")) {
            Platform.runLater(() -> {
                String script = "function getTextNodesIn(node) {\n"
                        + "    var textNodes = [];\n"
                        + "    if (node.nodeType == 3) {\n"
                        + "        textNodes.push(node);\n"
                        + "    } else {\n"
                        + "        var children = node.childNodes;\n"
                        + "        for (var i = 0, len = children.length; i < len; ++i) {\n"
                        + "            textNodes.push.apply(textNodes, getTextNodesIn(children[i]));\n"
                        + "        }\n"
                        + "    }\n"
                        + "    return textNodes;\n"
                        + "}\n"
                        + "\n"
                        + "function setSelectionRange(el, start, end) {\n"
                        + "    if (document.createRange && window.getSelection) {\n"
                        + "        var range = document.createRange();\n"
                        + "        range.selectNodeContents(el);\n"
                        + "        var textNodes = getTextNodesIn(el);\n"
                        + "        var foundStart = false;\n"
                        + "        var charCount = 0, endCharCount;\n"
                        + "\n"
                        + "        for (var i = 0, textNode; textNode = textNodes[i++]; ) {\n"
                        + "            endCharCount = charCount + textNode.length;\n"
                        + "            if (!foundStart && start >= charCount && (start < endCharCount || (start == endCharCount && i < textNodes.length))) {\n"
                        + "                range.setStart(textNode, start - charCount);\n"
                        + "                foundStart = true;\n"
                        + "            }\n"
                        + "            if (foundStart && end <= endCharCount) {\n"
                        + "                range.setEnd(textNode, end - charCount);\n"
                        + "                break;\n"
                        + "            }\n"
                        + "            charCount = endCharCount;\n"
                        + "        }\n"
                        + "\n"
                        + "        var sel = window.getSelection();\n"
                        + "        sel.removeAllRanges();\n"
                        + "        sel.addRange(range);\n"
                        + "    } else if (document.selection && document.body.createTextRange) {\n"
                        + "        var textRange = document.body.createTextRange();\n"
                        + "        textRange.moveToElementText(el);\n"
                        + "        textRange.collapse(true);\n"
                        + "        textRange.moveEnd(\"character\", end);\n"
                        + "        textRange.moveStart(\"character\", start);\n"
                        + "        textRange.select();\n"
                        + "    }\n"
                        + "}"
                        + "\n"
                        + "function makeEditableAndHighlight(colour, ttext) {\n"
                        + "    sel = window.getSelection();\n"
                        + "    document.designMode = \"on\";\n"
                        + "    document.execCommand('styleWithCSS', false, true);"
                        + "    // Use HiliteColor since some browsers apply BackColor to the whole block\n"
                        + "    if (!document.execCommand(\"HiliteColor\", false, colour)) {\n"
                        + "        document.execCommand(\"BackColor\", false, colour);\n"
                        + "    }\n"
                        + "    sel.getRangeAt(0).startContainer.parentNode.style.fontStyle = \"italic\";"
                        + "    sel.getRangeAt(0).startContainer.parentNode.title=ttext;"
                        + "    sel.collapseToEnd();"
                        + "    document.designMode = \"off\";\n"
                        + "}\n"
                        + "\n"
                        + "function highlightAndToolTip(color, ttext) {\n"
                        + "    var range, sel;\n"
                        + "    if (window.getSelection) {\n"
                        + "        // IE9 and non-IE\n"
                        + "        try {\n"
                        + "            if (!document.execCommand(\"BackColor\", false, color)) {\n"
                        + "                makeEditableAndHighlight(color, ttext);\n"
                        + "            }\n"
                        + "        } catch (ex) {\n"
                        + "            makeEditableAndHighlight(color, ttext)\n"
                        + "        }\n"
                        + "    } else if (document.selection && document.selection.createRange) {\n"
                        + "        // IE <= 8 case\n"
                        + "        range = document.selection.createRange();\n"
                        + "        range.execCommand(\"BackColor\", false, color);\n"
                        + "    }\n"
                        + "}"
                        + "\n"
                        + "setSelectionRange(document.getElementsByTagName(\"font\")[0], " + start
                        + ", " + stop + ");\n"
                        + "highlightAndToolTip(\"" + hexColor + "\", \"" + tip + "\");";

                browser.mainFrame().get().executeJavaScript(script);
            });
        }
    }
}