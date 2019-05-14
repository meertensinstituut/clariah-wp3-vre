FROM vucltl/vu-rm-pip3

RUN apt-get update && apt-get install -y --no-install-recommends vim virtualenv net-tools virtualenv
RUN virtualenv --python=python3 clamenv
RUN . clamenv/bin/activate && pip install clam flask lxml requests requests-oauthlib PyYAML

RUN . clamenv/bin/activate && clamnewproject vupipeline

ADD ./conf/run-pipeline-dkr.sh /vurm/scripts/run-pipeline-dkr.sh
ADD ./conf/config.yml /vurm/config.yml

ADD ./conf/vupipeline.py /vurm/vupipeline/vupipeline/vupipeline.py
ADD ./conf/vupipeline_wrapper.py /vurm/vupipeline/vupipeline/vupipeline_wrapper.py
ADD ./conf/formats.py /vurm/clamenv/lib/python3.6/site-packages/clam/common/formats.py

RUN . clamenv/bin/activate && cd vupipeline && python setup.py install
