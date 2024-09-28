package com.tobiga.spatiotemporal.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;

public interface SplitStrategy {
    List<RTreeNode> splitNode(RTreeNode node);
}
