package cook.actor.impl

import cook.actor.ConfigLoader
import cook.actor.ConfigManager
import cook.actor.ConfigRefLoader
import cook.actor.ConfigRefManager
import cook.actor.ConfigRefVerifier
import cook.actor.TargetManager
import cook.actor.TargetBuilder

import akka.actor.{ TypedActor, TypedProps }

trait TypedActorBase {

  protected def configRefLoader =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[ConfigRefLoader],
      TypedActor.context.system.actorFor("ConfigRefLoader"))

  protected def configRefVerifier =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[ConfigRefVerifier],
      TypedActor.context.system.actorFor("ConfigRefVerifier"))

  protected def configRefManager =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[ConfigRefManager],
      TypedActor.context.system.actorFor("ConfigRefManager"))

  protected def configLoader =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[ConfigLoader],
      TypedActor.context.system.actorFor("ConfigLoader"))

  protected def configManager =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[ConfigManager],
      TypedActor.context.system.actorFor("ConfigManager"))

  protected def targetManager =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[TargetManager],
      TypedActor.context.system.actorFor("TargetManager"))

  protected def targetBuilder =
    TypedActor(TypedActor.context.system).typedActorOf(
      TypedProps[TargetBuilder],
      TypedActor.context.system.actorFor("TargetBuilder"))
}