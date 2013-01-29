/**
 * Copyright (C) 2010 Orbeon, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation; either version
 * 2.1 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * The full text of the license is available at http://www.gnu.org/copyleft/lesser.html
 */
package org.orbeon.oxf.xforms.action.actions

import org.orbeon.oxf.xforms.XFormsConstants._
import org.orbeon.oxf.xforms.XFormsModel
import org.orbeon.oxf.xforms.action.{DynamicActionContext, XFormsAction}
import org.orbeon.oxf.xforms.event.{XFormsEvent, Dispatch}
import org.orbeon.oxf.xforms.event.events.{XFormsRevalidateEvent, XFormsRecalculateEvent, XFormsRebuildEvent}
import org.dom4j.QName

class XFormsRebuildAction extends RRRAction {
    def setFlag(model: XFormsModel, applyDefaults: Boolean)     = model.getDeferredActionContext.rebuild = true
    def createEvent(model: XFormsModel, applyDefaults: Boolean) = new XFormsRebuildEvent(model)
}

class XFormsRecalculateAction extends RRRAction {
    def setFlag(model: XFormsModel, applyDefaults: Boolean) = {
        model.getDeferredActionContext.recalculate = true
        if (applyDefaults)
            model.getBinds.resetFirstCalculate()
    }
    def createEvent(model: XFormsModel, applyDefaults: Boolean) = new XFormsRecalculateEvent(model, applyDefaults)
}

class XFormsRevalidateAction extends RRRAction {
    def setFlag(model: XFormsModel, applyDefaults: Boolean)     = model.getDeferredActionContext.revalidate = true
    def createEvent(model: XFormsModel, applyDefaults: Boolean) = new XFormsRevalidateEvent(model)
}

// Common functionality
trait RRRAction extends XFormsAction {

    override def execute(context: DynamicActionContext): Unit = {

        val interpreter = context.interpreter
        val model       = interpreter.actionXPathContext.getCurrentModel

        def resolve(qName: QName) =
            (Option(interpreter.resolveAVT(context.element, qName)) getOrElse "false").toBoolean

        val deferred      = resolve(XXFORMS_DEFERRED_QNAME)
        val applyDefaults = resolve(XXFORMS_DEFAULTS_QNAME)

        // Set the flag in any case
        setFlag(model, deferred)

        // Perform the action immediately if needed
        // NOTE: XForms 1.1 and 2.0 say that no event should be dispatched in this case. It's a bit unclear what the
        // purpose of these events is anyway.
        if (! deferred)
            Dispatch.dispatchEvent(createEvent(model, applyDefaults))
    }

    def setFlag(model: XFormsModel, applyDefaults: Boolean)
    def createEvent(model: XFormsModel, applyDefaults: Boolean): XFormsEvent
}