package com.springbatch.config;

import com.springbatch.model.Student;
import com.springbatch.model.StudentDao;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;

import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

    @Autowired
    private StepBuilderFactory sbf;

    @Autowired
    private JobBuilderFactory jbf;

    @Autowired
    private StudentDao studentDao;

    @Value("${student.record.path}")
    private Resource studentCsv;

    @Bean
    public StudentJobListner listener() {
        return new StudentJobListner();
    }

    @Bean
    public Job jobBean(){
        return jbf.get("jobA")
                .incrementer(new RunIdIncrementer())
                .listener(listener())
                .start(steps())
                // .next(stepB())
                // .next(stepC())
                .build();
    }

    @Bean
    public Step steps(){
        //OLD STEPS
        return sbf.get("stepA")
                .<Student,Student>chunk(10)
                .reader(reader())
                .processor(processor())
                .writer(writer())
                .build();

       /* return new StepBuilder("jobStep")
                .<Student,Student>chunk(5)
                .reader(reader()) //READ
                .processor(processor()) // PROCESS
                .writer(writer()) // WRITE-DUMP into Database
                .build();*/
    }

    //For Reading the CSV
    @Bean
    public FlatFileItemReader<Student> reader()  {
        try {
            return new FlatFileItemReaderBuilder<Student>()
                    .name("studentData")
                    .resource(studentCsv) //CSV
                    .linesToSkip(1) // for skipping the first line of CSV as Headers
                    .delimited()// for comma separation of headers
                    .names("id" , "name" , "email" , "mobileNo" , "fees" , "date")
                    .targetType(Student.class)
                    .build();
        } catch (Exception exception){
            exception.printStackTrace();
            return null;
        }

        //OLD Reader
      /*  FlatFileItemReader<Student> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("/student.csv"));
        reader.setLineMapper(new DefaultLineMapper<Student>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setDelimiter(DELIMITER_COMMA);
                setNames("name","number","amount","discount","location");
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<Student>() {{
                setTargetType(Student.class);
            }});
        }});
        reader.setRecordSeparatorPolicy(new SimpleRecordSeparatorPolicy());
        return reader;*/
    }

    //For Processing the data
    @Bean
    public ItemProcessor<Student, Student> processor(){
        return student -> {
            System.out.println("fess: "+student.getFees());
            int fees = Integer.parseInt(student.getFees().trim());
            Double finalAmount = fees-((15/100.0)*fees);
            student.setDiscount(finalAmount+"");
            return student;
        };
    }

    // Saving-Dumping into the database
    @Bean
    public ItemWriter<Student> writer(){
        return invoices -> {
            System.out.println("Saving Student Records: " +invoices);
            studentDao.saveAll(invoices);
        };
    }

}
