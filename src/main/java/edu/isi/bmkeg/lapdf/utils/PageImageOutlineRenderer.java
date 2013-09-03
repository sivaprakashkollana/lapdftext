package edu.isi.bmkeg.lapdf.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.font.TextAttribute;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import edu.isi.bmkeg.lapdf.controller.LapdfMode;
import edu.isi.bmkeg.lapdf.model.Block;
import edu.isi.bmkeg.lapdf.model.ChunkBlock;
import edu.isi.bmkeg.lapdf.model.PageBlock;
import edu.isi.bmkeg.lapdf.model.WordBlock;
import edu.isi.bmkeg.lapdf.model.ordering.SpatialOrdering;

public class PageImageOutlineRenderer {
	
	private static TreeMap<Integer, String> colorMap = new TreeMap<Integer, String>();
	private static TreeMap<String, Integer> countMap = new TreeMap<String, Integer>();
	private static final int TYPE_UNCLASSIFIED_COLOR_CODE = 0xFBF5EF;

	public static void dumpPageImageToFile(PageBlock page, File outputFile, String label, int mode) 
			throws IOException {

		BufferedImage image = PageImageOutlineRenderer.createPageImage(page, label, mode);
		
		ImageIO.write(image, "png", outputFile);

	}

	public static BufferedImage createPageImage(PageBlock page, String label, int mode) 
			throws IOException {

		int width = page.getPageBoxWidth();
		int height = page.getPageBoxHeight();

		BufferedImage image = new BufferedImage(width, height,
				BufferedImage.TYPE_INT_ARGB);
		image.getGraphics().setColor(Color.white);
		image.getGraphics().fillRect(0, 0, width, height);
		image.getGraphics().setColor(Color.red);

		drawWord(width / 2 - 50, 10, image, Color.black,
				label + ":" + page.getPageNumber(), 10);

		List<ChunkBlock> cbList = page.getAllChunkBlocks(SpatialOrdering.MIXED_MODE);
		List<Block> list = new ArrayList<Block>(cbList);
		
		// renderBlockPerImage(list, image, fileName);
		renderBlockByColor(list, image, mode);

		List<WordBlock> wbList = page.getAllWordBlocks(SpatialOrdering.MIXED_MODE);
		list = new ArrayList<Block>(wbList);
		renderBlockByColor(list, image, mode);

		return image;

	}	
	
	private static void renderBlockByColor(
			List<Block> entityList,
			BufferedImage image, int mode) {

		for (Block block : entityList) {
			if (block instanceof ChunkBlock) {

				ChunkBlock chunky = (ChunkBlock) block;
				Integer colorCode = colorDecider(chunky.getType());

				if (colorMap.get(colorCode) == null) {
					if (colorCode == TYPE_UNCLASSIFIED_COLOR_CODE)
						colorMap.put(colorCode, Block.TYPE_UNCLASSIFIED);
					else
						colorMap.put(colorCode, chunky.getType());
				}

				if (colorCode == TYPE_UNCLASSIFIED_COLOR_CODE) {
					if (countMap.get(Block.TYPE_UNCLASSIFIED) == null) {
						countMap.put(Block.TYPE_UNCLASSIFIED, 1);
					} else {
						countMap.put(Block.TYPE_UNCLASSIFIED,
								countMap.get(Block.TYPE_UNCLASSIFIED) + 1);
					}
				} else {
					if (countMap.get(chunky.getType()) == null) {
						countMap.put(chunky.getType(), 1);
					} else {
						countMap.put(chunky.getType(),
								countMap.get(chunky.getType()) + 1);
					}
				}
				
				drawRectangle(chunky.getX1(), chunky.getY1(),
						chunky.getWidth(), chunky.getHeight(), image,
						Color.red, chunky, mode);
				
				/*
				String text = chunky.getMostPopularWordFont() + ";" 
						+ chunky.getMostPopularWordStyle();
				if( mode != LapdfMode.BLOCK_ONLY )
					text = chunky.getType();
				*/
				
				drawWord( chunky.getX1() - 20, 
						chunky.getY1() , 
						image, 
						Color.black,
						String.format("%4f", chunky.readDensity()), 
						12);
				
			} else if (block != null) {
				
				WordBlock w = (WordBlock) block;
				ChunkBlock chunky = (ChunkBlock) w.getContainer();
				PageBlock page = w.getPage();
								
				int hSpace = 3;
				int vSpace = 3;
				try {
					hSpace = page.getMostPopularWordHeightPage() / 2 + 
							page.getMostPopularHorizontalSpaceBetweenWordsPage();
					vSpace = page.getMostPopularWordHeightPage() / 2 + 
							page.getMostPopularVerticalSpaceBetweenWordsPage();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
				Integer colorCode = (mode == 1) ? colorDecider(chunky.getType())
						: 0x000000;

				drawRectangle(w.getX1(), w.getY1(),
						w.getWidth(), w.getHeight(),
						image, new Color(colorCode), block, mode);

				if( w.getFontStyle() != null) {
				
					String text = w.getFontStyle();
				
					text = text.substring(10, text.length()-2);

					int centroidX = Math.round( (w.getX1() + w.getX2()) / 2);
					int centroidY = Math.round( (w.getY1() + w.getY2()) / 2);
					/*int[] wLocalDistances = w.getLocalDistances();
					int dNorth = wLocalDistances[0];
					int dSouth = wLocalDistances[1];
					int dEast = wLocalDistances[2];
					int dWest = wLocalDistances[3];
					
					if( dNorth == 1000 )
						drawLine( centroidX, w.getY2(), 
								centroidX, w.getY2() + vSpace,
								1, image, Color.cyan);
				
					if( dSouth == 1000 )
						drawLine( centroidX, w.getY1(), 
								centroidX, w.getY1() - vSpace,
								1, image, Color.cyan);

					if( dEast == 1000 )
						drawLine( w.getX2(), centroidY, 
								w.getX2() + hSpace, centroidY, 
								1, image, Color.cyan);

					if( dWest == 1000 )
						drawLine( w.getX1(), centroidY, 
								w.getX1() - hSpace, centroidY, 
								1, image, Color.cyan);*/

					
					WordBlock[] wFlushArray = w.getFlushArray();
					WordBlock lu = wFlushArray[0];
					WordBlock ru = wFlushArray[1];
					WordBlock ld = wFlushArray[2];
					WordBlock rd = wFlushArray[3];
					
					if( lu != null )
						drawLine( w.getX1(), centroidY, 
								w.getX1(), w.getY1(),
								1, image, Color.green);
				
					if( ru != null )
						drawLine( w.getX2(), centroidY, 
								w.getX2(), w.getY1(),
								1, image, Color.green);

					if( ld != null )
						drawLine( w.getX1(), centroidY, 
								w.getX1(), w.getY2(),
								1, image, Color.green);

					if( rd != null)
						drawLine( w.getX2(), centroidY, 
								w.getX2(), w.getY2(),
								1, image, Color.green);

					drawWord( centroidX, 
							centroidY + 4, 
							image, 
							Color.red,
							String.format("%d", w.getOrderAddedToChunk()), 
							8);
				}

			}

		}

	}

	private static int colorDecider(String type) {
		
		if (Block.TYPE_METHODS_HEADING.equals(type)) {
			return 0x0000ff;
		} else if (Block.TYPE_METHODS_BODY.equals(type)) {
			return 0x008000;
		} else if (Block.TYPE_METHODS_SUBTITLE.equals(type)) {
			return 0x00bfff;
		} else if (Block.TYPE_RESULTS_HEADING.equals(type)) {
			return 0x800080;
		} else if (Block.TYPE_RESULTS_BODY.equals(type)) {
			return 0x800000;
		} else if (Block.TYPE_RESULTS_SUBTITLE.equals(type)) {
			return 0x7cfc00;
		} else if (Block.TYPE_REFERENCES_HEADING.equals(type)) {
			return 0xffff00;
		} else if (Block.TYPE_REFERENCES_BODY.equals(type)) {
			return 0xff69b4;
		} else if (Block.TYPE_DISCUSSION_HEADING.equals(type)) {
			return 0xff0000;
		} else if (Block.TYPE_DISCUSSION_BODY.equals(type)) {
			return 0xfa8072;
		} else if (Block.TYPE_DISCUSSION_SUBTITLE.equals(type)) {
			return 0xff4500;
		} else if (Block.TYPE_CONCLUSIONS_HEADING.equals(type)) {
			return 0xb8860b;
		} else if (Block.TYPE_CONCLUSIONS_BODY.equals(type)) {
			return 0xbc8f8f;
		} else if (Block.TYPE_CONCLUSIONS_SUBTITLE.equals(type)) {
			return 0xbdb76b;
		} else if (Block.TYPE_ACKNOWLEDGEMENTS_HEADING.equals(type)) {
			return 0x4b0082;
		} else if (Block.TYPE_ACKNOWLEDGEMENTS_BODY.equals(type)) {
			return 0x556b2f;
		} else if (Block.TYPE_ABSTRACT_HEADING.equals(type)) {
			return 0xA9D0F5;
		} else if (Block.TYPE_ABSTRACT_BODY.equals(type)) {
			return 0x00FFFF;
		} else if (Block.TYPE_TITLE.equals(type)) {
			return 0xdc143c;
		} else if (Block.TYPE_AUTHORS.equals(type)) {
			return 0xffa500;
		} else if (Block.TYPE_INTRODUCTION_HEADING.equals(type)) {
			return 0xcd853f;
		} else if (Block.TYPE_INTRODUCTION_BODY.equals(type)) {
			return 0xd2691e;
		} else if (Block.TYPE_INTRODUCTION_SUBTITLE.equals(type)) {
			return 0xd2b48c;
		} else if (Block.TYPE_SUPPORTING_INFORMATION_HEADING.equals(type)) {
			return 0x8b4513;
		} else if (Block.TYPE_SUPPORTING_INFORMATION_BODY.equals(type)) {
			return 0x8fbc8f;
		} else if (Block.TYPE_SUPPORTING_INFORMATION_SUBTITLE.equals(type)) {
			return 0x90ee90;
		} else if (Block.TYPE_FIGURE_LEGEND.equals(type)) {
			return 0x000000;
		} else if (Block.TYPE_AFFLIATION.equals(type)) {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		} else if (Block.TYPE_HEADER.equals(type)) {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		} else if (Block.TYPE_FOOTER.equals(type)) {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		} else if (Block.TYPE_KEYWORDS.equals(type)) {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		} else if (Block.TYPE_TABLE.equals(type)) {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		} else if (Block.TYPE_CITATION.equals(type)) {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		} else {
			return TYPE_UNCLASSIFIED_COLOR_CODE;
		}

	}

	private static void drawRectangle(int x, int y, int width, int height,
			BufferedImage image, Color color, Block block, int mode) {

		Graphics2D graphics = image.createGraphics();

		graphics.setPaint(color);

		Rectangle rect = new Rectangle(x, y, width, height);
		if ( block instanceof WordBlock && mode == LapdfMode.BLOCK_ONLY ) {

			graphics.draw(rect);
			graphics.fill(rect);
			
		} else {

			graphics.draw(rect);

		}

	}

	private static void drawWord(int x, int y, int width, int height,
			BufferedImage image, Color color, WordBlock word) {
		
		Graphics2D graphics = image.createGraphics();
		Font plainFont = new Font(word.getFont(), Font.PLAIN, word.getHeight());

		AttributedString as = new AttributedString(word.getWord());
		as.addAttribute(TextAttribute.FONT, plainFont);

		graphics.setPaint(color);
		graphics.drawString(as.getIterator(), x, y);

	}
	
	private static void drawLine(
			int x1, int y1, 
			int x2, int y2, 
			int weight,
			BufferedImage image,
			Color color) {
		
		Graphics2D graphics = image.createGraphics();
		graphics.setPaint(color);
		graphics.drawLine(x1, y1, x2, y2);

	}

	private static void drawWord(
			int x, int y, 
			BufferedImage image,
			Color color, 
			String word,
			int size) {
		
		Graphics2D graphics = image.createGraphics();
		Font plainFont = new Font(Font.SANS_SERIF, Font.PLAIN, size);
		AttributedString as = new AttributedString(word);
		as.addAttribute(TextAttribute.FONT, plainFont);
		graphics.setPaint(color);
		graphics.drawString(as.getIterator(), x, y);

	}

	public static void createReport(String fileName) {

		if (colorMap.size() == 0 || countMap.size() == 0) {
			throw new IllegalStateException(
					"Before calling this method you should use createPageImage " +
					"to draw the individual pages");
		}

		BufferedImage image = new BufferedImage(500, 800,
				BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		image.getGraphics().setColor(Color.white);
		image.getGraphics().fillRect(0, 0, 500, 800);
		String sectionType;
		int x = 20;
		int y = 20;
		Font plainFont = new Font(Font.SANS_SERIF, Font.PLAIN, 20);
		for (Integer color : colorMap.keySet()) {
			sectionType = colorMap.get(color);
			Shape circle = new Ellipse2D.Float(x, y, 20, 20);
			graphics.setPaint(new Color(color));
			graphics.draw(circle);
			graphics.fill(circle);
			graphics.setColor(Color.black);
			AttributedString as = new AttributedString(sectionType);
			as.addAttribute(TextAttribute.FONT, plainFont);
			graphics.drawString(as.getIterator(), x + 22, y + 20);
			as = new AttributedString(countMap.get(sectionType) + "");

			graphics.drawString(as.getIterator(), 380, y + 20);

			x = 20;
			y = y + 24;
		}

		try {

			File outputfile = new File(fileName);
			ImageIO.write(image, "png", outputfile);
		} catch (IOException e) {

		}
		colorMap.clear();
		countMap.clear();
	
	}

}
