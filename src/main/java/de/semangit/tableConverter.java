package de.semangit;

import com.opencsv.CSVReader;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class tableConverter {
    private static String input;
    private static String output;
    private static Integer blocksize;

    private static void usage()
    {
        String jarname = new java.io.File(tableConverter.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();
        System.out.println("Usage: java -jar " + jarname + " -in=INPUT.csv -out=OUTPUT.csv -blocksize=INTEGER");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length > 2) {
            for (String s: args){
                s = s.toLowerCase();
                if(s.contains("-in="))
                {
                    input = s.substring(s.lastIndexOf("=") + 1);

                }
                else if(s.contains(("-out=")))
                {
                    output = s.substring(s.lastIndexOf("=") + 1);
                }
                else if(s.contains(("-blocksize=")))
                {
                    blocksize = Integer.parseInt(s.substring(s.lastIndexOf("=") + 1));
                }
            }

            if(input == null || output == null || blocksize == null)
            {
                usage();
            }

            try {
                CSVReader reader = new CSVReader(new FileReader(input));
                BufferedWriter w = new BufferedWriter(new FileWriter(output), 32768);
                String[] nextLine;
                ArrayList<String[]> relevantLines = new ArrayList<>();
                String currentId = "-1";
                String currentYear = "-1";
                Map<String, ArrayList<Integer>> languageAllocation = new HashMap<>();

                while((nextLine = reader.readNext()) != null)
                {
                    if(relevantLines.size() == 0) { //only during start up
                        relevantLines.add(nextLine);
                        currentId = nextLine[0];
                        currentYear = nextLine[1];
                    }
                    else
                    {
                        //check if next line is of same kind as previous - same project same time
                        if(nextLine[0].equals(currentId) && nextLine[1].equals(currentYear))
                        {
                            relevantLines.add(nextLine);
                        }
                        else
                        {
                            int ctr = 0;
                            //for each language (first parameter), store the ids in which they are returned/printed
                            if(languageAllocation.isEmpty()) //first year for this project
                            {

                                for (String[] s : relevantLines) {
                                    if(languageAllocation.containsKey(s[2]))
                                    {
                                        System.out.println("Warning - duplicate detected: ID: " + currentId + ", year: " + currentYear + ", lang: " + s[2]);
                                        continue;
                                    }
                                    int numEntriesForLanguage = Integer.parseInt(s[3]) / blocksize;
                                    ArrayList<Integer> allocs = new ArrayList<>();
                                    for(int i = 0; i < numEntriesForLanguage; i++)
                                    {
                                        allocs.add(ctr++);
                                    }
                                    languageAllocation.put(s[2], allocs);
                                }
                            }
                            else //consecutive year for same project.
                            {
                                //compare size of new allocs with old ones
                                ArrayList<Integer> unallocated = new ArrayList<>();

                                //language has grown and requires more entries
                                Map<String, Integer> moreRequired = new HashMap<>();
                                for (String[] s : relevantLines) {
                                    int numEntriesForLanguage = Integer.parseInt(s[3]) / blocksize;
                                    if(!languageAllocation.containsKey(s[2]))
                                    {
                                        moreRequired.put(s[2], numEntriesForLanguage);
                                    }
                                    else
                                    {
                                        //size last year - size this year
                                        int sizeDifference = languageAllocation.get(s[2]).size() - numEntriesForLanguage;
                                        if(sizeDifference > 0)
                                        {

                                            //language has decreased in size!
                                            for(int i = 0; i < sizeDifference; i++)
                                            {
                                                //saving that we removed an element from the allocation list
                                                ArrayList<Integer> realloc = languageAllocation.get(s[2]);
                                                unallocated.add(realloc.remove(realloc.size() - 1));
                                                languageAllocation.put(s[2], realloc);
                                            }
                                        }
                                        else
                                        {
                                            moreRequired.put(s[2], -sizeDifference);
                                        }
                                    }
                                }
                                //do we have to append more to the list? if so, need to grab unused index
                                int highestIndex = -1;
                                if(moreRequired.size() > unallocated.size())
                                {
                                    for(Map.Entry<String, ArrayList<Integer>> entry : languageAllocation.entrySet())
                                    {
                                        for (int i : entry.getValue()) {
                                            if( i > highestIndex)
                                            {
                                                highestIndex = i;
                                            }
                                        }
                                    }
                                }

                                //loop over moreRequired
                                for(Map.Entry<String, Integer> entry : moreRequired.entrySet()) {
                                    String key = entry.getKey();
                                    int value = entry.getValue();
                                    ArrayList<Integer> currentAllocs = languageAllocation.get(key);
                                    if(currentAllocs == null)
                                    {
                                        currentAllocs = new ArrayList<>();
                                    }
                                    for(int i = 0; i < value; i++)
                                    {
                                        if(!unallocated.isEmpty())
                                        {
                                            currentAllocs.add(unallocated.remove(unallocated.size() - 1));
                                        }
                                        else
                                        {
                                            currentAllocs.add(++highestIndex);
                                            //append
                                        }
                                    }
                                    languageAllocation.put(key, currentAllocs);
                                }


                            }

                            //print new allocations
                            for(Map.Entry<String, ArrayList<Integer>> entry : languageAllocation.entrySet()) {
                                for (int i: entry.getValue()) {
                                    w.write(currentId + "." + i + "," + currentYear + "," + entry.getKey());
                                    w.newLine();
                                }
                            }

                            if(!nextLine[0].equals(currentId)) //same project, but year complete
                            {
                                languageAllocation.clear();
                            }



                            //got a bunch of entities belonging to the same project
                            relevantLines.clear();
                            relevantLines.add(nextLine);
                            currentId = nextLine[0];
                            currentYear = nextLine[1];
                        }
                    }
                }
                //TODO: handle last line


                w.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
                System.exit(1);
            }
        }
        else
        {
            System.out.println("Three parameters. input, output, block size"); //todo
        }
    }
}
