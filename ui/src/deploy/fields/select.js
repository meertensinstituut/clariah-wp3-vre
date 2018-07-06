import React from "react";
import {FormControl, FormGroup} from 'react-bootstrap';
import RemoveButton from "./remove-button";

export default class Select extends React.Component {

    constructor(props) {
        super(props);
        this.state = {
            options: this.props.param.values.map((v) => v.value)
        };
    }

    static handleValidation(value, options) {
        if (value === "") return null;
        if (options.includes(value)) return 'success';
        return 'error';
    };

    render() {
        let multiple = false;
        return (
            <FormGroup
                controlId="formControlsSelect"
                validationState={Select.handleValidation(this.props.value, this.state.options)}
            >
                <div className="input-group mb-3">
                    <FormControl.Feedback/>
                    <FormControl
                        componentClass="select"
                        placeholder={this.props.value}
                        multiple={multiple}
                        onChange={(e) => this.props.onChange(e.target.value)}
                        value={this.props.value}
                    >
                        <option value="empty-field">---</option>
                        {this.props.param.values.map(function (value, i) {
                            return (
                                <option key={i} value={value.value}>
                                    {value.label}
                                    {(value.label !== undefined && value.description !== undefined) ? " - " : ""}
                                    {value.description}
                                </option>
                            );
                        }, this)}
                    </FormControl>
                    <div className="input-group-append">
                        <RemoveButton
                            canRemove={this.props.canRemove}
                            onRemove={this.props.onRemove}
                            nextToInput={true}
                        />
                    </div>
                </div>
            </FormGroup>
        );
    }
}