package com.supersonic

import ai.x.play.json.Jsonx
import play.api.libs.json.Format

case class DependencyGraph(commit: String, modules: List[Module])

object DependencyGraph {
  implicit val fmt: Format[DependencyGraph] = Jsonx.formatCaseClass[DependencyGraph]
}
