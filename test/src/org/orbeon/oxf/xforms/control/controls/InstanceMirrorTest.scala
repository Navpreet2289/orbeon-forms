/**
 * Copyright (C) 2011 Orbeon, Inc.
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
package org.orbeon.oxf.xforms.control.controls

import XXFormsDynamicControl._
import org.junit.Test
import org.orbeon.oxf.test.DocumentTestBase
import org.orbeon.oxf.xforms.control.controls.InstanceMirror._
import org.orbeon.oxf.xforms.event.{EventListener ⇒ JEventListener}
import org.scalatest.junit.AssertionsForJUnit

class InstanceMirrorTest extends DocumentTestBase with AssertionsForJUnit {

    @Test def mirrorInSamePart(): Unit = {
        this setupDocument
            <xh:html xmlns:xh="http://www.w3.org/1999/xhtml"
                     xmlns:xf="http://www.w3.org/2002/xforms"
                     xmlns:ev="http://www.w3.org/2001/xml-events"
                     xmlns:xxf="http://orbeon.org/oxf/xml/xforms">

                <xh:head>
                    <xf:model id="model">
                        <xf:instance id="form-instance" xxf:exclude-result-prefixes="#all">
                            <xh:html>
                                <xh:head>
                                    <xf:model id="my-model">
                                        <xf:instance id="my-instance">
                                            <instance/>
                                        </xf:instance>
                                    </xf:model>
                                </xh:head>
                                <xh:body/>
                            </xh:html>
                        </xf:instance>
                        
                        <xf:instance id="my-instance" xxf:exclude-result-prefixes="#all">
                            <instance xmlns=""/>
                        </xf:instance>

                        <xf:insert   ev:event="update1"  context="//xf:instance/instance"   origin="xf:element('first', 'Arthur')"/>
                        <xf:insert   ev:event="update2"  ref="//xf:instance/instance/first" origin="xf:element('last', 'Clark')"    position="after"/>
                        <xf:insert   ev:event="update3"  ref="//xf:instance/instance/last"  origin="xf:element('middle', 'C.')"     position="before"/>
                        <xf:setvalue ev:event="update4"  ref="//xf:instance/instance/last">Clarke</xf:setvalue>
                        <xf:delete   ev:event="update5"  ref="//xf:instance/instance/*"/>
                        <xf:insert   ev:event="update6"  context="//xf:instance/instance"   origin="xf:attribute('first', 'Arthur')"/>
                        <xf:insert   ev:event="update7"  ref="//xf:instance/instance/@*"    origin="xf:attribute('last', 'Clarke')" position="after"/>
                        <xf:setvalue ev:event="update8"  ref="//xf:instance/instance/@last">Conan Doyle</xf:setvalue>
                        <xf:delete   ev:event="update9"  ref="//xf:instance/instance/@*"/>
                        <xf:insert   ev:event="update10" context="/*/xh:body"               origin="xf:element('xh:div')"/>
                        <xf:setvalue ev:event="update11" ref="/*/xh:body/xh:div">Hello!</xf:setvalue>

                        <xf:delete   ev:event="update12" ref="//xf:instance/instance"/>
                        <xf:insert   ev:event="update13" context="//xf:instance"            origin="xf:element('root')"/>

                    </xf:model>
                </xh:head>
                <xh:body/>
            </xh:html>

        implicit val logger = document.getIndentedLogger

        val outerInstance = document.findInstance("form-instance").get
        val innerInstance = document.findInstance("my-instance").get

        var nonInstanceChanges = 0

        // Attach listeners
        val outerListener: JEventListener = {

            val unknownChange: EventListener = _ ⇒ {
                nonInstanceChanges += 1
                true
            }

            val outerMirrorListener =
                mirrorListener(document, toInnerInstanceNode(outerInstance.documentInfo, document.getStaticState.topLevelPart, document, findOuterInstanceDetailsDynamic))

            composeListeners(Seq(outerMirrorListener, unknownChange))
        }

        addListener(outerInstance, outerListener)

        // Test insert, value change and delete
        val expected = Seq(
            ("""<instance><first>Arthur</first></instance>""", 0),
            ("""<instance><first>Arthur</first><last>Clark</last></instance>""" , 0),
            ("""<instance><first>Arthur</first><middle>C.</middle><last>Clark</last></instance>""" , 0),
            ("""<instance><first>Arthur</first><middle>C.</middle><last>Clarke</last></instance>""" , 0),
            ("""<instance/>""" , 0),
            ("""<instance first="Arthur"/>""" , 0),
            ("""<instance first="Arthur" last="Clarke"/>""" , 0),
            ("""<instance first="Arthur" last="Conan Doyle"/>""" , 0),
            ("""<instance/>""" , 0),
            ("""<instance/>""" , 1),
            ("""<instance/>""" , 2)
            // NOTE: Removing root element doesn't work right now, maybe because doDelete() doesn't support it
//            ("""""" , 2),
//            ("""<root/>""" , 2)
        )

        expected.zipWithIndex foreach {
            case ((expectedInnerInstanceValue, expectedNonInstanceChanges), index) ⇒
                // Dispatch event and assert result
                dispatch("update" + (index + 1), "model")

                assert(instanceAsString(innerInstance) === expectedInnerInstanceValue)
                assert(nonInstanceChanges === expectedNonInstanceChanges)
        }
    }

    @Test def mirrorXBL(): Unit = {

        this setupDocument
            <xh:html xmlns:xh="http://www.w3.org/1999/xhtml"
                     xmlns:xf="http://www.w3.org/2002/xforms"
                     xmlns:ev="http://www.w3.org/2001/xml-events"
                     xmlns:xxf="http://orbeon.org/oxf/xml/xforms"
                     xmlns:xbl="http://www.w3.org/ns/xbl"
                     xmlns:xxbl="http://orbeon.org/oxf/xml/xbl"
                     xmlns:fr="http://orbeon.org/oxf/xml/form-runner">

                <xh:head>
                    <xf:model id="model">
                        <xf:instance id="outer-instance" xxf:exclude-result-prefixes="#all">
                            <instance/>
                        </xf:instance>

                        <xf:insert   ev:event="update1" context="instance()"   origin="xf:element('first', 'Arthur')"/>
                        <xf:insert   ev:event="update2" ref="instance()/first" origin="xf:element('last', 'Clark')"    position="after"/>
                        <xf:insert   ev:event="update3" ref="instance()/last"  origin="xf:element('middle', 'C.')"     position="before"/>
                        <xf:setvalue ev:event="update4" ref="instance()/last">Clarke</xf:setvalue>
                        <xf:delete   ev:event="update5" ref="instance()/*"/>
                        <xf:insert   ev:event="update6" context="instance()"   origin="xf:attribute('first', 'Arthur')"/>
                        <xf:insert   ev:event="update7" ref="instance()/@*"    origin="xf:attribute('last', 'Clarke')" position="after"/>
                        <xf:setvalue ev:event="update8" ref="instance()/@last">Conan Doyle</xf:setvalue>
                        <xf:delete   ev:event="update9" ref="instance()/@*"/>

                    </xf:model>
                    <xbl:xbl>
                        <xbl:binding id="fr-gaga" element="fr|gaga" xxbl:mode="binding">
                            <xbl:template>
                                <xf:model id="gaga-model">
                                    <xf:instance id="gaga-instance" xxbl:mirror="true">
                                        <empty/>
                                    </xf:instance>

                                    <xf:insert   ev:event="update1" context="instance()"   origin="xf:element('first', 'Arthur')"/>
                                    <xf:insert   ev:event="update2" ref="instance()/first" origin="xf:element('last', 'Clark')"    position="after"/>
                                    <xf:insert   ev:event="update3" ref="instance()/last"  origin="xf:element('middle', 'C.')"     position="before"/>
                                    <xf:setvalue ev:event="update4" ref="instance()/last">Clarke</xf:setvalue>
                                    <xf:delete   ev:event="update5" ref="instance()/*"/>
                                    <xf:insert   ev:event="update6" context="instance()"   origin="xf:attribute('first', 'Arthur')"/>
                                    <xf:insert   ev:event="update7" ref="instance()/@*"    origin="xf:attribute('last', 'Clarke')" position="after"/>
                                    <xf:setvalue ev:event="update8" ref="instance()/@last">Conan Doyle</xf:setvalue>
                                    <xf:delete   ev:event="update9" ref="instance()/@*"/>
                                </xf:model>
                            </xbl:template>
                        </xbl:binding>
                    </xbl:xbl>
                </xh:head>
                <xh:body>
                    <fr:gaga id="my-gaga" ref="instance()"/>
                </xh:body>
            </xh:html>

        val outerInstance = document.findInstanceInDescendantOrSelf("outer-instance").get
        val innerInstance = document.findInstanceInDescendantOrSelf("gaga-instance").get

        // Test insert, value change and delete
        val expected = Seq(
            """<instance><first>Arthur</first></instance>""",
            """<instance><first>Arthur</first><last>Clark</last></instance>""",
            """<instance><first>Arthur</first><middle>C.</middle><last>Clark</last></instance>""",
            """<instance><first>Arthur</first><middle>C.</middle><last>Clarke</last></instance>""",
            """<instance/>""",
            """<instance first="Arthur"/>""",
            """<instance first="Arthur" last="Clarke"/>""",
            """<instance first="Arthur" last="Conan Doyle"/>""",
            """<instance/>"""
        )

        var updates = 0

        // First update outer instance and check inner instance, then do the reverse
        for ((targetPrefixedId, mirroredInstance) ← Seq("model" → innerInstance, "my-gaga$gaga-model" → outerInstance))
            expected.zipWithIndex foreach {
                case (expectedInstanceValue, index) ⇒
                    // Dispatch event and assert result
                    dispatch("update" + (index + 1), targetPrefixedId)
                    assert(instanceAsString(mirroredInstance) === expectedInstanceValue)
                    updates += 1
            }

        assert(updates === expected.size * 2)
    }
}