package ome.services.utests;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ome.model.annotations.ImageAnnotation;
import ome.model.containers.Dataset;
import ome.model.containers.Project;
import ome.model.core.Image;
import ome.services.util.CountCollector;

import junit.framework.TestCase;


public class CountCollectorTest extends TestCase
{

    protected CountCollector c;
   
    protected long current = 0;
    
    protected Long next() { return current++; }
    
    public void testSingleField() throws Exception
    {
        c = new CountCollector(new String[]{Dataset.ANNOTATIONS});
        
        Project p = new Project(next());
        Dataset d = new Dataset(next());
        p.addDataset(d);
        
        c.collect(p);
        Set s = (Set) c.getIds(Dataset.ANNOTATIONS);
        
        assertTrue(s.contains(d.getId()));
    }
    
    public void testMultipleFields() throws Exception
    {
        c = new CountCollector(new String[]{Dataset.IMAGELINKS,Image.ANNOTATIONS});
        
        Project p = new Project(next());
        Dataset d = new Dataset(next());
        p.addDataset(d);
        
        Image i = new Image(next());
        d.addImage(i);
        
        ImageAnnotation iann = new ImageAnnotation(next());
        Set annotations = new HashSet();
        i.setAnnotations(annotations);
        i.getAnnotations().add(iann);
        
        c.collect(p);
        Set s_1 = (Set) c.getIds(Dataset.IMAGELINKS);
        Set s_2 = (Set) c.getIds(Image.ANNOTATIONS);
        
        assertTrue(s_1.contains(d.getId()));
        assertTrue(s_2.contains(i.getId()));
        
    }
    
    public void testMultipleIdsInOneField() throws Exception
    {
        
        c = new CountCollector(new String[]{Image.CATEGORYLINKS});
        
        Dataset d = new Dataset(next());
        Image i1 = new Image(next());
        Image i2 = new Image(next());
        d.addImage(i1);
        d.addImage(i2);
        
        c.collect(d);
        Set s = (Set) c.getIds(Image.CATEGORYLINKS);
        assertTrue(s.size() == 2);
        assertTrue(s.contains(i1.getId()));
        assertTrue(s.contains(i2.getId()));
    }
    
    public void testLookupTablesCreated() throws Exception
    {
         c = new CountCollector(new String[]{Project.DATASETLINKS});
         
         Project p = new Project(next());
         c.collect(p);
         c.addCounts(Project.DATASETLINKS,p.getId(),10);
    }
    
    public void testNoCountGiven() throws Exception
    {
        c = new CountCollector(new String[]{Project.DATASETLINKS});
        
        Project p = new Project(next());
        c.collect(p);
        c.addCounts(Project.DATASETLINKS,p.getId(),null);
    }    

    public void testNegativeCountGiven() throws Exception
    {
        c = new CountCollector(new String[]{Project.DATASETLINKS});
        
        Project p = new Project(next());
        c.collect(p);
        c.addCounts(Project.DATASETLINKS,p.getId(),-10);
    }    
    
    public void testWhatHappensOnNullIdThough() throws Exception
    {
        c = new CountCollector(new String[]{Project.DATASETLINKS});
        
        Project p = new Project();
        
        c.collect(p);
    }
    
}
