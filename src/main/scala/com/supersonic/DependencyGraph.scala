package com.supersonic

import ai.x.play.json.Jsonx

case class DependencyGraph(commit: String, modules: List[Module])

object DependencyGraph {
  implicit val fmt = Jsonx.formatCaseClass[DependencyGraph]
}
