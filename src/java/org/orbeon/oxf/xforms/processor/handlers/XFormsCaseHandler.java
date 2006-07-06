/**
 *  Copyright (C) 2006 Orbeon, Inc.
 *
 *  This program is free software; you can redistribute it and/or modify it under the terms of the
 *  GNU Lesser General Public License as published by the Free Software Foundation; either version
 *  2.1 of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Lesser General Public License for more details.
 *
 *  The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.processor.handlers;

import org.orbeon.oxf.xml.*;
import org.orbeon.oxf.xforms.processor.XFormsElementFilterContentHandler;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

import java.util.Map;

/**
 * Handle xforms:case.
 */
public class XFormsCaseHandler extends HandlerBase {

    private DeferredContentHandler currentSavedOutput;
    private OutputInterceptor currentOutputInterceptor;
    private String currentCaseEffectiveId;

    public XFormsCaseHandler() {
        super(false, true);
    }

    public void start(String uri, String localname, String qName, Attributes attributes) throws SAXException {
        currentCaseEffectiveId = handlerContext.getEffectiveId(attributes);

        // Find classes to add
        final StringBuffer classes = new StringBuffer("xforms-" + localname);

        final AttributesImpl newAttributes = getAttributes(attributes, classes.toString(), currentCaseEffectiveId);

        final Map switchIdToSelectedCaseIdMap = containingDocument.getXFormsControls().getCurrentControlsState().getSwitchIdToSelectedCaseIdMap();

//        final String selectedCaseId = (String) switchIdToSelectedCaseIdMap.get(effectiveGroupId);
//        final boolean isVisible = currentCaseEffectiveId.equals(selectedCaseId);

        // TODO: This is probably not efficient, but we don't have the switch id right here
        final boolean isVisible = switchIdToSelectedCaseIdMap.containsValue(currentCaseEffectiveId);

        newAttributes.addAttribute("", "style", "style", ContentHandlerHelper.CDATA, "display: " + (isVisible ? "block" : "none"));

        final String xhtmlPrefix = handlerContext.findXHTMLPrefix();
        final String spanQName = XMLUtils.buildQName(xhtmlPrefix, "span");

        // Place interceptor
        currentSavedOutput = handlerContext.getController().getOutput();
        currentOutputInterceptor = new OutputInterceptor(currentSavedOutput, spanQName, new OutputInterceptor.Listener() {
            public void generateFirstDelimiter(OutputInterceptor outputInterceptor) throws SAXException {
                // Output begin delimiter
                outputInterceptor.outputDelimiter(currentSavedOutput, outputInterceptor.getDelimiterNamespaceURI(),
                        outputInterceptor.getDelimiterPrefix(), outputInterceptor.getDelimiterLocalName(), "xforms-case-begin-end", "xforms-case-begin-" + currentCaseEffectiveId);
            }
        });

        currentOutputInterceptor.setAddedClasses(new StringBuffer(isVisible ? "xforms-case-selected" : "xforms-case-deselected"));

        handlerContext.getController().setOutput(new DeferredContentHandlerImpl(new XFormsElementFilterContentHandler(currentOutputInterceptor)));
        setContentHandler(handlerContext.getController().getOutput());
    }

    public void end(String uri, String localname, String qName) throws SAXException {
        currentOutputInterceptor.flushCharacters(true, true);

        // Restore output
        handlerContext.getController().setOutput(currentSavedOutput);
        setContentHandler(currentSavedOutput);

        if (currentOutputInterceptor.getDelimiterNamespaceURI() != null) {
            // Output end delimiter
            currentOutputInterceptor.outputDelimiter(currentSavedOutput, currentOutputInterceptor.getDelimiterNamespaceURI(),
                currentOutputInterceptor.getDelimiterPrefix(), currentOutputInterceptor.getDelimiterLocalName(), "xforms-case-begin-end", "xforms-case-end-" + currentCaseEffectiveId);
        } else {
            // Output start and end delimiter using xhtml:span
            final String xhtmlPrefix = handlerContext.findXHTMLPrefix();
            currentOutputInterceptor.outputDelimiter(currentSavedOutput, XMLConstants.XHTML_NAMESPACE_URI,
                xhtmlPrefix, "span", "xforms-case-begin-end", "xforms-case-begin-" + currentCaseEffectiveId);
            currentOutputInterceptor.outputDelimiter(currentSavedOutput, XMLConstants.XHTML_NAMESPACE_URI,
                xhtmlPrefix, "span", "xforms-case-begin-end", "xforms-case-end-" + currentCaseEffectiveId);
        }
    }
}
