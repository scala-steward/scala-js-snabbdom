/*
 * Copyright 2022 buntec
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2015 Simon Friis Vindum
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

package snabbdom.modules

import snabbdom._
import org.scalajs.dom

object EventListeners {

  val module: Module = Module().copy(
    create = Some(new CreateHook {
      override def apply(vNode: PatchedVNode): Unit = {

        val listener = createListener(vNode)
        val elm = vNode.elm

        vNode.data.on.foreach { case (name, _) =>
          elm.addEventListener(name, listener.jsFun, false)
        }

      }
    }),
    postPatch = Some(new PostPatchHook {
      override def apply(
          oldVNode: PatchedVNode,
          vNode: PatchedVNode
      ): PatchedVNode =
        updateEventListeners(oldVNode, vNode)
    }),
    destroy = Some(new DestroyHook {
      override def apply(vnode: PatchedVNode): Unit = {

        val oldListener = vnode.listener
        val oldOn = vnode.data.on
        val elm = vnode.elm

        if (oldOn.nonEmpty && oldListener.isDefined) {
          val ol = oldListener.get
          oldOn.foreach { case (name, _) =>
            elm.removeEventListener(name, ol.jsFun, false)
          }
        }

      }
    })
  )

  private def createListener(vnode: PatchedVNode) = {
    new Listener(vnode.toVNode)
  }

  private def updateEventListeners(
      oldVnode: PatchedVNode,
      vnode: PatchedVNode
  ): PatchedVNode = {

    val oldOn = oldVnode.data.on
    val oldListener = oldVnode.listener
    val elm = oldVnode.elm.asInstanceOf[dom.Element]
    val on = vnode.data.on

    if (oldOn != on) {

      if (oldOn.nonEmpty && oldListener.isDefined) {
        val ol = oldListener.get
        if (on.isEmpty) {
          oldOn.foreach { case (name, _) =>
            elm.removeEventListener(name, ol.jsFun, false)
          }
        } else {
          oldOn.foreach { case (name, _) =>
            if (on.get(name).isEmpty) {
              elm.removeEventListener(name, ol.jsFun, false)
            }
          }
        }
      }

      if (on.nonEmpty) {

        val listener = oldListener.getOrElse(createListener(vnode))

        if (oldOn.isEmpty) {
          on.foreach { case (name, _) =>
            elm.addEventListener(name, listener.jsFun, false)
          }
        } else {
          on.foreach { case (name, _) =>
            if (!oldOn.contains(name)) {
              elm.addEventListener(name, listener.jsFun, false)
            }
          }
        }

        val vnode1 = vnode.copy(listener = Some(listener))
        listener.vnode = vnode1.toVNode
        vnode1

      } else {
        vnode
      }

    } else {
      vnode
    }

  }

}
