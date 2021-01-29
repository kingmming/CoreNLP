package edu.stanford.nlp.parser.shiftreduce;

import java.util.List;

import edu.stanford.nlp.parser.common.ParserConstraint;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.TreeShapedStack;

/**
 * A version of the BinaryTransition that removes the unary
 * transitions from the left part of the BinaryTransition.
 * <br>
 * Note that right version of this BinaryTransition doesn't exist
 * because there would be no reason to first UnaryTransition, then
 * immediately BinaryTransition and remove that UnaryTransition.
 * <br>
 * Although if we create the version of the Binary that affects the
 * stack spots 2/3 down, perhaps the additional information gotten
 * from that will reveal that a right side UnaryTransition was
 * wrong... seems a bit much though
 */
public class BinaryRemoveUnaryTransition extends BinaryTransition {
  public BinaryRemoveUnaryTransition(String label, Side side, boolean isRoot) {
    super(label, side, isRoot);
  }

  /**
   * All the rules the for the BinaryTransition being legal still apply.
   * <br>
   * Also need to check that there are unary transitions to be
   * removed.  Furthermore, if removing a unary transition ruins a
   * constraint, that is not allowed.
   */
  public boolean isLegal(State state, List<ParserConstraint> constraints) {
    if (!super.isLegal(state, constraints)) {
      return false;
    }

    // there can only be unary transitions to remove if the top node
    // is a unary
    Tree prevNode = state.stack.pop().peek();
    if (prevNode.children().length > 1) {
      return false;
    }

    // there were no unary transitions if the node in question is a
    // preterminal or leaf
    if (prevNode.isLeaf() || prevNode.isPreTerminal()) {
      return false;
    }

    if (constraints == null) {
      return true;
    }

    // TODO: test this
    final int prevLeft = ShiftReduceUtils.leftIndex(prevNode);
    final int prevRight = ShiftReduceUtils.rightIndex(prevNode);
    Tree prevBottom = prevNode;
    while (prevBottom.children().length == 1 && !prevBottom.children()[0].isPreTerminal()) {
      prevBottom = prevBottom.children()[0];
    }
    for (ParserConstraint constraint : constraints) {
      if (prevLeft == constraint.start && prevRight == constraint.end - 1) {
        // this constraint matched the shape of the tree.  check that
        // it doesn't stop matching if we remove the unary transitions
        if (ShiftReduceUtils.constraintMatchesTreeTop(prevNode, constraint) &&
            !ShiftReduceUtils.constraintMatchesTreeTop(prevBottom, constraint)) {
          return false;
        }
      }
    }

    return true;
  }

  /**
   * Does the same thing as the parent class, in fact calling the
   * parent class, but only after removing the UnaryNodes from the
   * left part of the binary transition
   */
  @Override
  public State apply(State state, double scoreDelta) {
    TreeShapedStack<Tree> stack = state.stack;
    Tree right = stack.peek();
    stack = stack.pop();
    Tree left = stack.peek();
    stack = stack.pop();

    // find the bottom of the unary chain
    while (left.children().length == 1 && !left.isPreTerminal()) {
      left = left.children()[0];
    }

    stack = stack.push(left);
    stack = stack.push(right);

    // then use the new nodes to make a new state, as if the unaries had never existed...
    state = new State(stack, state.transitions, state.separators, state.sentence, state.tokenPosition, state.score, false);
    // ... and use that state to apply the base BinaryTransition
    state = super.apply(state, scoreDelta);
    Transition lastTransition = state.transitions.peek();
    if (!(lastTransition instanceof BinaryRemoveUnaryTransition)) {
      throw new AssertionError("I suppose this() is not actually the subclass when in a super() method");
    }
    return state;
  }


  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (!(o instanceof BinaryRemoveUnaryTransition)) {
      return false;
    }
    String otherLabel = ((BinaryRemoveUnaryTransition) o).label;
    Side otherSide = ((BinaryRemoveUnaryTransition) o).side;
    return otherSide.equals(side) && label.equals(otherLabel);
  }

  @Override
  public int hashCode() {
    switch(side) {
    case LEFT:
      return 59874523 ^ label.hashCode();
    case RIGHT:
      return 13543213 ^ label.hashCode();
    default:
      throw new IllegalArgumentException("Unknown side " + side);
    }
  }

  @Override
  public String toString() {
    switch(side) {
    case LEFT:
      return "LeftBinaryRemoveUnary" + (isRoot ? "*" : "") + "(" + label + ")";
    case RIGHT:
      return "RightBinaryRemoveUnary" + (isRoot ? "*" : "") + "(" + label + ")";
    default:
      throw new IllegalArgumentException("Unknown side " + side);
    }
  }

  private static final long serialVersionUID = 1;  
}

