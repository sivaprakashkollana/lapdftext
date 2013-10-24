package edu.isi.bmkeg.lapdf.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math3.stat.clustering.Cluster;
import org.apache.commons.math3.stat.clustering.KMeansPlusPlusClusterer;
import org.apache.log4j.Logger;

import edu.isi.bmkeg.lapdf.extraction.JPedalExtractor;
import edu.isi.bmkeg.lapdf.extraction.exceptions.InvalidPopularSpaceValueException;
import edu.isi.bmkeg.lapdf.features.HorizontalSplitFeature;
import edu.isi.bmkeg.lapdf.model.Block;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.LapdfDocument;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.factory.AbstractModelFactory;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;
import edu.isi.bmkeg.lapdf.model.spatial.SpatialEntity;
import edu.isi.bmkeg.lapdf.utils.PageImageOutlineRenderer;
import edu.isi.bmkeg.utils.FrequencyCounter;
import edu.isi.bmkeg.utils.IntegerFrequencyCounter;

public class RuleBasedParser implements Parser {

	private static Logger logger = Logger.getLogger(RuleBasedParser.class);

	private boolean debugImages = false;

	private ArrayList<PageBlock> pageList;
	
	private JPedalExtractor pageExtractor;
	
	private int idGenerator;
	
	private IntegerFrequencyCounter avgHeightFrequencyCounter;

	private FrequencyCounter fontFrequencyCounter;
	
	private int northSouthSpacing;
	
	private int eastWestSpacing;
	
	protected AbstractModelFactory modelFactory;
	
	protected String path;

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	private boolean isDebugImages() {
		return debugImages;
	}

	private void setDebugImages(boolean debugImages) {
		this.debugImages = debugImages;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public RuleBasedParser(AbstractModelFactory modelFactory)
			throws Exception {

		pageList = new ArrayList<PageBlock>();
		pageExtractor = new JPedalExtractor(modelFactory);

		idGenerator = 1;
		this.avgHeightFrequencyCounter = new IntegerFrequencyCounter(1);
		this.fontFrequencyCounter = new FrequencyCounter();

		this.modelFactory = modelFactory;

	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public LapdfDocument parse(File file) 
			throws Exception {

		LapdfDocument document = null;

		init(file);
		List<WordBlock> pageWordBlockList = null;
		PageBlock pageBlock = null;
		int pageCounter = 1;

		document = new LapdfDocument(file);
		document.setjPedalDecodeFailed(true);

		String pth = file.getPath();
		pth = pth.substring(0, pth.lastIndexOf(".pdf"));
		File imgDir = new File(pth);
		if (isDebugImages()) {
			imgDir.mkdir();
		}
		
		//
		// Calling 'hasNext()' get the text from JPedal.
		// 
		while (pageExtractor.hasNext()) {
			
			document.setjPedalDecodeFailed(false);
			
			pageBlock = modelFactory.createPageBlock(
					pageCounter++,
					pageExtractor.getCurrentPageBoxWidth(),
					pageExtractor.getCurrentPageBoxHeight(), 
					document);
			
			pageList.add(pageBlock);
			
			pageWordBlockList = pageExtractor.next();
			if(pageWordBlockList.isEmpty())
				continue;

			idGenerator = pageBlock.initialize(pageWordBlockList, idGenerator);

			this.eastWestSpacing = (pageBlock.getMostPopularWordHeightPage()) / 2
					+ pageBlock.getMostPopularHorizontalSpaceBetweenWordsPage();
						
			this.northSouthSpacing = (pageBlock.getMostPopularWordHeightPage() ) / 2
					+ pageBlock.getMostPopularVerticalSpaceBetweenWordsPage();

			buildChunkBlocks(pageWordBlockList, pageBlock);
			
			if (isDebugImages()) {
				PageImageOutlineRenderer.dumpChunkTypePageImageToFile(
						pageBlock,
						new File(pth + "/_01_afterBuildBlocks" + pageBlock.getPageNumber() + ".png"),
						file.getName() + "afterBuildBlocks"
								+ pageBlock.getPageNumber() + ".png");
			}

		}

		//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		
		if (!document.hasjPedalDecodeFailed()) {

			// initial parse is commplete. 
			String s = file.getName().replaceAll("\\.pdf", "");
			Pattern p = Pattern.compile("(\\d+)");
			Matcher m = p.matcher(file.getName());
			if( m.find() ) {
				s = m.group(1);
			}

			/*for (PageBlock page : pageList) {
				
				if (isDebugImages()) {
					PageImageOutlineRenderer.dumpPageImageToFile(page, 
							new File(pth + "/_02_beforeBuildBlocksOverlapDeletion_" + page.getPageNumber() + ".png"),
							file.getName() + "beforeBuildBlocksOverlapDeletion_"
									+ s + "_" + page.getPageNumber()
									+ ".png", 0);
				}

				this.deleteHighlyOverlappedChunkBlocks(page);

				if (isDebugImages()) {
					PageImageOutlineRenderer.dumpPageImageToFile(page, 
							new File(pth + "/_03_afterBuildBlocksOverlapDeletion_" + page.getPageNumber() + ".png"),
							file.getName() + "afterBuildBlocksOverlapDeletion_"
									+ s + "_" + page.getPageNumber()
									+ ".png", 0);
				}

				this.divideBlocksVertically(page);

				if (isDebugImages()) {

					PageImageOutlineRenderer.dumpPageImageToFile(page, 
							new File(pth + "/_04_afterVerticalDivide_" + page.getPageNumber() + ".png"),
							file.getName() + "afterVerticalDivide_" + s
									+ "_" + page.getPageNumber() + ".png", 0);
				}

				this.joinLines(page);

				if (isDebugImages()) {
					PageImageOutlineRenderer.dumpPageImageToFile(page, 
							new File(pth + "/_05_afterJoinLines_" + page.getPageNumber() + ".png"),
							file.getName() + "afterJoinLines_" + s + "_"
									+ page.getPageNumber() + ".png", 0);
				}

				this.divideBlocksHorizontally(page);

				if (isDebugImages()) {
					PageImageOutlineRenderer.dumpPageImageToFile(page, 
							new File(pth + "/_06_afterHorizontalDivide_" + page.getPageNumber() + ".png"),
							file.getName() + "afterHorizontalDivide_" + s
									+ "_" + page.getPageNumber() + ".png", 0);
				}

				this.deleteHighlyOverlappedChunkBlocks(page);

				if (isDebugImages()) {
					PageImageOutlineRenderer.dumpPageImageToFile(page, 
							new File(pth + "/_07_afterOverlapDeletion_" + page.getPageNumber() + ".png"),
							file.getName() + "/afterOverlapDeletion_" + s
									+ "_" + page.getPageNumber() + ".png", 0);
				}

			}*/

			document.addPages(pageList);

			document.calculateBodyTextFrame();
			document.calculateMostPopularFontStyles();

		}

		return document;
	}

	private void init(File file) throws Exception {

		pageExtractor.init(file);
		idGenerator = 1;
		
		this.avgHeightFrequencyCounter.reset();
		this.fontFrequencyCounter.reset();

		pageList.clear();

	}	
	
	private void buildChunkBlocks(List<WordBlock> wordBlocksLeftInPage,
			PageBlock page) {

		LinkedBlockingQueue<WordBlock> wordBlocksLeftToCheckInChunk = 
				new LinkedBlockingQueue<WordBlock>();
		
		List<WordBlock> chunkWords = new ArrayList<WordBlock>();
		List<WordBlock> rotatedWords = new ArrayList<WordBlock>();
		int counter;
		List<ChunkBlock> chunkBlockList1 = new ArrayList<ChunkBlock>();

		while (wordBlocksLeftInPage.size() > 0) {
			
			wordBlocksLeftToCheckInChunk.clear();
			
			// Start off with this word block
			wordBlocksLeftToCheckInChunk.add(wordBlocksLeftInPage.get(0));

			counter = 0;
			int extra;
			
			// Here are all the words we've come across in this run 
			chunkWords.clear();
			int chunkTextHeight = -1;
			
			// Build a single Chunk here based on overlapping words
			// keep going while there are still words to work through
			while (wordBlocksLeftToCheckInChunk.size() != 0) {
				
				//
				// get the top of the stack of words in the queue,
				// note that we have not yet looked at this word
				//
				// look at the top word in the queue
				WordBlock word = wordBlocksLeftToCheckInChunk.peek();
				
				if( chunkTextHeight == -1 ) {
					chunkTextHeight = word.getHeight();
				}
				
				// add this word's height and font to the counts.
				page.getDocument().getAvgHeightFrequencyCounter().add(
						word.getHeight());
				page.getDocument().getFontFrequencyCounter().add(
						word.getFont() + ";" + word.getFontStyle() );
				
				// remove this word from the global search
				wordBlocksLeftInPage.remove(word);

				// what other words on the page are close to this word 
				// and are still in the block?				
				List<WordBlock> wordsToAddThisIteration = word.readNearbyWords(
						this.eastWestSpacing, this.northSouthSpacing);				
					
				// TODO how to add more precise word features without
				word.writeFlushArray(wordsToAddThisIteration);
				
				wordsToAddThisIteration.retainAll(wordBlocksLeftInPage);
				
				// remove the words we've already looked at
				wordsToAddThisIteration.removeAll(wordBlocksLeftToCheckInChunk);
				
				// or they've already been seen
				wordsToAddThisIteration.removeAll(chunkWords);
				
				//
				// TODO Add criteria here to improve blocking by 
				// dropping newly found words that should be excluded.
				//
				//List<WordBlock> wordsToKill = new ArrayList<WordBlock>();
				/*for( WordBlock w : wordsToAddThisIteration) {

					// or they are a different height from the height 
					// of the first word in this chunk +/- 1px.
					if( w.getHeight() > chunkTextHeight + 1 ||
							w.getHeight() < chunkTextHeight - 1 ) {
					//	wordsToKill.add(w);
					}

					// or does adding this word to the chunk to the existing rectangle 
					// significantly causes the word density to drop beyond that of 
					// adding a new row (or column) to the block? 

				}
				wordsToAddThisIteration.removeAll(wordsToKill);*/
				
				// At this point, these words will be added to this chunk.
				wordBlocksLeftToCheckInChunk.addAll(wordsToAddThisIteration);

				// get this word from the queue and add it.
				WordBlock wb = wordBlocksLeftToCheckInChunk.poll();
				chunkWords.add(wb);					
				wb.setOrderAddedToChunk(chunkWords.size());
			}

			wordBlocksLeftInPage.removeAll(chunkWords);
			
			ChunkBlock cb1 = buildChunkBlock(chunkWords, page);
			chunkBlockList1.add(cb1);
			
		}

		idGenerator = page.addAll(new ArrayList<SpatialEntity>(
				chunkBlockList1), idGenerator);

	}

	private void divideBlocksVertically(PageBlock page)
			throws InvalidPopularSpaceValueException {

		List<ChunkBlock> chunkBlockList;
		String leftRightMidline;
		boolean leftFlush;
		boolean rightFlush;

		chunkBlockList = new ArrayList<ChunkBlock>(page.getAllChunkBlocks(null));

		for (ChunkBlock chunky : chunkBlockList) {
			
			leftRightMidline = chunky.readLeftRightMidLine();
			leftFlush = chunky.isFlush(ChunkBlock.LEFT,
					chunky.getMostPopularWordHeight() * 2);
			
			rightFlush = chunky.isFlush(ChunkBlock.RIGHT,
					chunky.getMostPopularWordHeight() * 2);
			
			int deltaH = chunky.getMostPopularWordHeight()
					- page.getDocument().readMostPopularWordHeight();
			
			if (ChunkBlock.MIDLINE.equalsIgnoreCase(leftRightMidline)
					&& (leftFlush || rightFlush) && deltaH < 3) {
				if (verticalSplitCandidate(chunky))
					this.splitBlockDownTheMiddle(chunky);
			}
		
		}

	}

	private boolean verticalSplitCandidate(ChunkBlock block)
			throws InvalidPopularSpaceValueException {

		// 0:x,1:width
		ArrayList<Integer[]> spaceList = new ArrayList<Integer[]>();
		int prevX = 0;

		int prevW = 0;
		int currX = 0;
		int currY = 0;
		int currW = 0;
		Integer[] currSpace = new Integer[] { -100, -100 };
		Integer[] currWidest = new Integer[] { -100, -100 };

		PageBlock parent = (PageBlock) block.getContainer();
		List<SpatialEntity> wordBlockList = parent.containsByType(block,
				SpatialOrdering.MIXED_MODE, WordBlock.class);
		
		int pageWidth = parent.getMargin()[2] - parent.getMargin()[0];
		int marginHeight = parent.getMargin()[3] - parent.getMargin()[1];
		int averageWidth = 0;
		float spaceWidthToPageWidth = 0;

		for (int i = 0; i < wordBlockList.size(); i++) {
			WordBlock wb = (WordBlock) wordBlockList.get(i);

			// New line started
			if (i == 0 || 
					Math.abs(((double) (wb.getY1() - currY) / (double) marginHeight)) > 0.01) {

				currY = wb.getY1();
				currX = wb.getX1();
				currW = wb.getWidth();
				if (currWidest[1] > 0) {
					spaceList.add(new Integer[] { currWidest[0],
							currWidest[1] });
				}
				currWidest[0] = -100;
				currWidest[1] = -100;
				continue;
			}

			// Continuing current line
			prevX = currX;
			prevW = currW;
			currY = wb.getY1();
			currX = wb.getX1();
			currW = wb.getWidth();
			currSpace[1] = currX - (prevX + prevW);
			currSpace[0] = currX + currW;

			if (currWidest[1] == -100
					|| currSpace[1] > currWidest[1]) {
				currWidest[0] = currSpace[0];
				currWidest[1] = currSpace[1];
			}
		}

		// Criterium for whether the widest spaces are properly lined up:
		// At least 20% of them have an x position within that differ with less
		// than 1% to the x position of the previous space.
		// The average x position doesn't matter!
		if (spaceList.size() <= 0)
			return false;

		// Find average width of the widest spaces and make sure it's at least
		// as wide as 2.5% of the page width.
		for (int i = 0; i < spaceList.size(); i++)
			averageWidth += spaceList.get(i)[1];
		averageWidth = averageWidth / spaceList.size();
		// spaceWidthToPageWidth = (float) averageWidth / (float) pageWidth;

		/*
		 * if (spaceWidthToPageWidth > 0.015) return true; else return false;
		 */
		if (averageWidth > parent.getMostPopularHorizontalSpaceBetweenWordsPage())
			return true;
		else
			return false;
	}

	private void splitBlockDownTheMiddle(ChunkBlock block) {

		PageBlock parent = (PageBlock) block.getContainer();
		int median = parent.getMedian();
		ArrayList<WordBlock> leftBlocks = new ArrayList<WordBlock>();
		ArrayList<WordBlock> rigthBlocks = new ArrayList<WordBlock>();
		List<SpatialEntity> wordBlockList = parent.containsByType(block,
				SpatialOrdering.MIXED_MODE, WordBlock.class);
		String wordBlockLeftRightMidLine;
		for (int i = 0; i < wordBlockList.size(); i++) {
			WordBlock wordBlock = (WordBlock) wordBlockList.get(i);
			wordBlockLeftRightMidLine = wordBlock.readLeftRightMidLine();

			if (wordBlockLeftRightMidLine.equals(Block.LEFT))
				leftBlocks.add(wordBlock);
			else if (wordBlockLeftRightMidLine.equals(Block.RIGHT))
				rigthBlocks.add(wordBlock);
			else if (wordBlockLeftRightMidLine.equals(Block.MIDLINE)) {
				// Assign the current word to the left or right side depending
				// upon
				// whether most of the word is on the left or right side of the
				// median.

				if (Math.abs(median - wordBlock.getX1()) > Math.abs(wordBlock
						.getX2() - median)) {
					wordBlock.resize(wordBlock.getX1(), wordBlock.getY1(),
							median - wordBlock.getX1(), wordBlock.getHeight());

				} else {
					wordBlock.resize(median, wordBlock.getY1(),
							wordBlock.getX2() - median, wordBlock.getHeight());
					rigthBlocks.add(wordBlock);

				}

			}
		}// END for

		if (leftBlocks.size() == 0 || rigthBlocks.size() == 0)
			return;
		
		ChunkBlock leftChunkBlock = buildChunkBlock(leftBlocks, parent);
		ChunkBlock rightChunkBlock = buildChunkBlock(rigthBlocks, parent);

		SpatialEntity entity = modelFactory.createWordBlock(
				leftChunkBlock.getX2() + 1, leftChunkBlock.getY1(),
				rightChunkBlock.getX1() - 1, rightChunkBlock.getY2(), 0, null,
				null, null);
		if (parent.intersectsByType(entity, null, WordBlock.class).size() >= 1) {
			if (block == null) {
				logger.info("null null");
			}
			for (SpatialEntity wordBlockEntity : wordBlockList)
				((Block) wordBlockEntity).setContainer(block);

			return;
		}

		double relative_overlap = leftChunkBlock
				.getRelativeOverlap(rightChunkBlock);
		if (relative_overlap < 0.1) {
			parent.delete(block, block.getId());
			parent.add(leftChunkBlock, idGenerator++);
			parent.add(rightChunkBlock, idGenerator++);
		}

	}

	private ChunkBlock buildChunkBlock(List<WordBlock> wordBlockList,
			PageBlock pageBlock) {

		ChunkBlock chunkBlock = null;
		
		IntegerFrequencyCounter lineHeightFrequencyCounter = new IntegerFrequencyCounter(1);
		IntegerFrequencyCounter spaceFrequencyCounter = new IntegerFrequencyCounter(0);
		FrequencyCounter fontFrequencyCounter = new FrequencyCounter();
		FrequencyCounter styleFrequencyCounter = new FrequencyCounter();

		for (WordBlock wordBlock : wordBlockList) {
			
			lineHeightFrequencyCounter.add(wordBlock.getHeight());
			spaceFrequencyCounter.add(wordBlock.getSpaceWidth());

			avgHeightFrequencyCounter.add(wordBlock.getHeight());
			fontFrequencyCounter.add(wordBlock.getFont());
			styleFrequencyCounter.add(wordBlock.getFontStyle());
			
			if (chunkBlock == null) {

				chunkBlock = modelFactory
						.createChunkBlock(wordBlock.getX1(), wordBlock.getY1(),
								wordBlock.getX2(), wordBlock.getY2());

			} else {
			
				SpatialEntity spatialEntity = chunkBlock.union(wordBlock);
				chunkBlock.resize(spatialEntity.getX1(), spatialEntity.getY1(),
						spatialEntity.getWidth(), spatialEntity.getHeight());

			}

			wordBlock.setContainer(chunkBlock);

		}
		
		chunkBlock.setMostPopularWordFont(
				(String) fontFrequencyCounter.getMostPopular()
				);

		chunkBlock.setMostPopularWordStyle(
				(String) styleFrequencyCounter.getMostPopular()
				);
		
		chunkBlock.setMostPopularWordHeight(
				lineHeightFrequencyCounter.getMostPopular()
				);
		
		chunkBlock.setMostPopularWordSpaceWidth(
				spaceFrequencyCounter.getMostPopular()
				);
		
		chunkBlock.setContainer(pageBlock);
				
		return chunkBlock;

	}

	private void divideBlocksHorizontally(PageBlock page) {

		List<ChunkBlock> chunkBlockList;
		ArrayList<Integer> breaks;

		chunkBlockList = page.getAllChunkBlocks(SpatialOrdering.MIXED_MODE);
		for (ChunkBlock chunky : chunkBlockList) {
			breaks = this.getBreaks(chunky);
			if (breaks.size() > 0)
				this.splitBlockByBreaks(chunky, breaks);
		}

	}

	private ArrayList<Integer> getBreaks(ChunkBlock block) {
		ArrayList<Integer> breaks = new ArrayList<Integer>();
		PageBlock parent = (PageBlock) block.getContainer();

		int mostPopulareWordHeightOverCorpora = parent.getDocument()
				.readMostPopularWordHeight();

		List<SpatialEntity> wordBlockList = parent.containsByType(block,
				SpatialOrdering.MIXED_MODE, WordBlock.class);

		WordBlock firstWordOnLine = (WordBlock) wordBlockList.get(0);
		WordBlock lastWordOnLine = firstWordOnLine;

		int lastY = firstWordOnLine.getY1() + firstWordOnLine.getHeight() / 2;
		int currentY = lastY;

		String chunkBlockString = "";

		ArrayList<Integer> breakCandidates = new ArrayList<Integer>();

		ArrayList<HorizontalSplitFeature> featureList = new ArrayList<HorizontalSplitFeature>();
		HorizontalSplitFeature feature = new HorizontalSplitFeature();
		for (SpatialEntity entity : wordBlockList) {
			lastY = currentY;
			WordBlock wordBlock = (WordBlock) entity;
			currentY = wordBlock.getY1() + wordBlock.getHeight() / 2;

			if (currentY > lastY + wordBlock.getHeight() / 2) {
				feature.calculateFeatures(block, firstWordOnLine,
						lastWordOnLine, chunkBlockString);
				featureList.add(feature);
				feature = new HorizontalSplitFeature();
				breakCandidates
						.add((lastWordOnLine.getY2() + wordBlock.getY1()) / 2);

				firstWordOnLine = wordBlock;
				lastWordOnLine = wordBlock;
				chunkBlockString = "";

			}
			feature.addToFrequencyCounters(wordBlock.getFont(),
					wordBlock.getFontStyle());
			chunkBlockString = chunkBlockString + " " + wordBlock.getWord();
			lastWordOnLine = wordBlock;
		}
		feature.calculateFeatures(block, firstWordOnLine, lastWordOnLine,
				chunkBlockString);
		featureList.add(feature);
		feature = null;
		HorizontalSplitFeature featureMinusOne;

		// What kind of column is this?
		//
		// a. Titles and large-font blocks
		// b. centered titles
		// c. centered blocks
		// d. text & titles in left or right columns
		// e. references
		// f. figure legends

		for (int i = 1; i < featureList.size(); i++) {
			featureMinusOne = featureList.get(i - 1);
			feature = featureList.get(i);

			if (featureMinusOne.isAllCapitals() && !feature.isAllCapitals()) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (!featureMinusOne.isAllCapitals()
					&& feature.isAllCapitals()) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (featureMinusOne.getMostPopularFont() != null
					&& feature.getMostPopularFont() == null) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (featureMinusOne.getMostPopularFont() == null
					&& feature.getMostPopularFont() != null) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (!featureMinusOne.getMostPopularFont().equals(
					feature.getMostPopularFont())
					&& !feature.isMixedFont() && !featureMinusOne.isMixedFont()) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (Math.abs(feature.getFirstWordOnLineHeight()
					- featureMinusOne.getFirstWordOnLineHeight()) > 2) {
				breaks.add(breakCandidates.get(i - 1));

			} else if (Math.abs(feature.getMidYOfLastWordOnLine()
					- featureMinusOne.getMidYOfLastWordOnLine()) > (feature
					.getFirstWordOnLineHeight() + featureMinusOne
					.getFirstWordOnLineHeight()) * 0.75) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (Math.abs(featureMinusOne.getFirstWordOnLineHeight()
					- mostPopulareWordHeightOverCorpora) <= 2
					&& Math.abs(feature.getFirstWordOnLineHeight()
							- mostPopulareWordHeightOverCorpora) <= 2
					&& Math.abs(featureMinusOne.getMidOffset()) < 10
					&& Math.abs(featureMinusOne.getExtremLeftOffset()) > 10
					&& Math.abs(featureMinusOne.getExtremeRightOffset()) > 10
					&& Math.abs(feature.getExtremLeftOffset()) < 20
					&& Math.abs(feature.getExtremeRightOffset()) < 10) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (Math.abs(feature.getFirstWordOnLineHeight()
					- mostPopulareWordHeightOverCorpora) <= 2
					&& Math.abs(feature.getMidOffset()) < 10
					&& Math.abs(feature.getExtremLeftOffset()) > 10
					&& Math.abs(feature.getExtremeRightOffset()) > 10
					&& Math.abs(featureMinusOne.getExtremLeftOffset()) < 10) {
				breaks.add(breakCandidates.get(i - 1));
			} else if (featureMinusOne.isEndOFLine()
					&& Math.abs(featureMinusOne.getFirstWordOnLineHeight()
							- mostPopulareWordHeightOverCorpora) <= 2
					&& (Math.abs(featureMinusOne.getExtremeRightOffset()) > 10 || Math
							.abs(feature.getExtremLeftOffset()) > 10)) {
				breaks.add(breakCandidates.get(i - 1));
			}

		}

		return breaks;
	}

	private void splitBlockByBreaks(ChunkBlock block, ArrayList<Integer> breaks) {

		Collections.sort(breaks);
		PageBlock parent = (PageBlock) block.getContainer();

		List<SpatialEntity> wordBlockList = parent.containsByType(block,
				SpatialOrdering.MIXED_MODE, WordBlock.class);

		int y;
		int breakIndex;
		List<List<WordBlock>> bigBlockList = new ArrayList<List<WordBlock>>();
		for (int j = 0; j < breaks.size() + 1; j++) {
			List<WordBlock> littleBlockList = new ArrayList<WordBlock>();
			bigBlockList.add(littleBlockList);
		}

		for (SpatialEntity entity : wordBlockList) {
			WordBlock wordBlock = (WordBlock) entity;
			y = wordBlock.getY1() + wordBlock.getHeight() / 2;
			breakIndex = Collections.binarySearch(breaks, y);
			if (breakIndex < 0) {
				breakIndex = -1 * breakIndex - 1;
				bigBlockList.get(breakIndex).add(wordBlock);

			} else {
				bigBlockList.get(breakIndex).add(wordBlock);
			}
		}
		ChunkBlock chunky;
		TreeSet<ChunkBlock> chunkBlockList = new TreeSet<ChunkBlock>(
				new SpatialOrdering(SpatialOrdering.MIXED_MODE));
		for (List<WordBlock> list : bigBlockList) {
			if (list.size() == 0)
				continue;
			chunky = this.buildChunkBlock(list, parent);
			chunkBlockList.add(chunky);

		}
		parent.delete(block, block.getId());
		idGenerator = parent.addAll(
				new ArrayList<SpatialEntity>(chunkBlockList), idGenerator);
	}

	private void joinLines(PageBlock page) {

		LinkedBlockingQueue<ChunkBlock> chunkBlockList = new LinkedBlockingQueue<ChunkBlock>(
				page.getAllChunkBlocks(SpatialOrdering.MIXED_MODE));
		List wordBlockList;

		int midY;
		ChunkBlock chunky = null;
		List<SpatialEntity> neighbouringChunkBlockList;
		ChunkBlock neighbouringChunkBlock;
		ArrayList<SpatialEntity> removalList = new ArrayList<SpatialEntity>();
		while (chunkBlockList.size() > 0) {

			chunky = chunkBlockList.peek();

			wordBlockList = page.containsByType(chunky, null, WordBlock.class);
			if (wordBlockList.size() < 4 && chunky.readNumberOfLine() == 1) {

				neighbouringChunkBlockList = page.intersectsByType(
						calculateBoundariesForJoin(chunky, page),
						SpatialOrdering.MIXED_MODE, ChunkBlock.class);

				if (neighbouringChunkBlockList.size() <= 1) {
					chunkBlockList.poll();
					continue;
				}
				
				for (SpatialEntity entity : neighbouringChunkBlockList) {
					neighbouringChunkBlock = (ChunkBlock) entity;
					if (neighbouringChunkBlock.equals(chunky))
						continue;
					midY = chunky.getY1() + chunky.getHeight() / 2;
					if (neighbouringChunkBlock.getY1() < midY
							&& neighbouringChunkBlock.getY2() > midY
							&& ((neighbouringChunkBlock.getX2() < chunky
									.getX1() && neighbouringChunkBlock
									.readNumberOfLine() < 3) || (neighbouringChunkBlock
									.getX1() > chunky.getX2() && neighbouringChunkBlock
									.readNumberOfLine() == 1))) {
						removalList.add(neighbouringChunkBlock);
						wordBlockList.addAll(page.containsByType(
								neighbouringChunkBlock, null, WordBlock.class));

					}
				}

				if (removalList.size() > 0) {

					ChunkBlock newChunkBlock = this.buildChunkBlock(
							wordBlockList, page);
					page.add(newChunkBlock, idGenerator++);
					page.delete(chunky, chunky.getId());
					chunkBlockList.removeAll(removalList);
					for (SpatialEntity forDeleteEntity : removalList) {
						page.delete(forDeleteEntity, forDeleteEntity.getId());
					}
				}

			}
			removalList.clear();

			chunkBlockList.poll();

		}

	}

	private SpatialEntity calculateBoundariesForJoin(ChunkBlock chunk,
			PageBlock parent) {

		SpatialEntity entity = null;
		int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
		int width = parent.getMargin()[2] - parent.getMargin()[0];
		int height = parent.getMargin()[3] - parent.getMargin()[1];
		String lrm = chunk.readLeftRightMidLine();

		width = (int) (width * 0.25);
		y1 = chunk.getY1();
		y2 = chunk.getY2();
		if (Block.LEFT.equalsIgnoreCase(lrm)) {
			// TODO:Use reflection

			x1 = (chunk.getX1() - width <= 0) ? parent.getMargin()[0] : chunk
					.getX1() - width;
			x2 = (chunk.getX2() + width >= parent.getMedian()) ? parent
					.getMedian() : chunk.getX2() + width;

			entity = modelFactory.createChunkBlock(x1, y1, x2, y2);

		} else if (Block.RIGHT.equalsIgnoreCase(lrm)) {

			x1 = (chunk.getX1() - width <= parent.getMedian()) ? parent
					.getMedian() : chunk.getX1() - width;
			x2 = (chunk.getX2() + width >= parent.getMargin()[2]) ? parent
					.getMargin()[2] : chunk.getX2() + width;

			entity = modelFactory.createChunkBlock(x1, y1, x2, y2);

		} else {

			x1 = (chunk.getX1() - width <= 0) ? parent.getMargin()[0] : chunk
					.getX1() - width;
			x2 = (chunk.getX2() + width >= parent.getMargin()[2]) ? parent
					.getMargin()[2] : chunk.getX2() + width;
			entity = modelFactory.createChunkBlock(x1, y1, x2, y2);
		}

		return entity;

	}
	
	private void deleteHighlyOverlappedChunkBlocks(PageBlock page) {
		
		List<ChunkBlock> chunkBlockList = page.getAllChunkBlocks(
				SpatialOrdering.MIXED_MODE);
		
		List<SpatialEntity> wordList;
		SpatialEntity intersectingRectangle;
		double property1, property2;

		for (SpatialEntity entity : chunkBlockList) {

			ChunkBlock chunky = (ChunkBlock) entity;

			List<SpatialEntity> neighbouringChunkBlockList = page.intersectsByType(chunky,
					SpatialOrdering.MIXED_MODE, ChunkBlock.class);

			for (SpatialEntity neighbourEntity : neighbouringChunkBlockList) {

				ChunkBlock neighbourChunk = (ChunkBlock) neighbourEntity;

				intersectingRectangle = chunky
						.getIntersectingRectangle(neighbourChunk);

				property1 = (intersectingRectangle.getHeight() * intersectingRectangle
						.getWidth())
						/ (double) (chunky.getWidth() * chunky.getHeight());

				property2 = (double) (intersectingRectangle.getHeight() * intersectingRectangle
						.getWidth())
						/ (double) (neighbourChunk.getWidth() * neighbourChunk
								.getHeight());

				if (property1 > property2 && property1 > 0.9) {
					wordList = page.containsByType(chunky, null,
							WordBlock.class);

					for (SpatialEntity wordEntity : wordList)
						((Block) wordEntity).setContainer(neighbourChunk);
					page.delete(chunky, chunky.getId());

				}

				if (property2 > property1 && property2 > 0.9) {
					wordList = page.containsByType(neighbourChunk, null,
							WordBlock.class);

					for (SpatialEntity wordEntity : wordList)
						((Block) wordEntity).setContainer(chunky);
					page.delete(neighbourChunk, neighbourChunk.getId());

				}

			}

		}

	}

}
